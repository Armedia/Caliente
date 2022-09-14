using System;
using System.Collections.Concurrent;
using System.Threading;

namespace Caliente.SharePoint.Common
{

    public interface PoolableObjectFactory<T>
    {
        T Create();
        void Destroy(T t);
        bool Validate(T t);
        void Activate(T t);
        void Passivate(T t);
    }

    public class ObjectPool<T> : IDisposable
    {
        public sealed class Ref : IDisposable
        {
            private ObjectPool<T> pool;
            private T target;
            private bool disposed = false;

            public Ref(T target, ObjectPool<T> pool)
            {
                this.pool = pool;
                this.target = target;
            }

            public T Target
            {
                get
                {
                    if (this.disposed)
                        throw new InvalidOperationException("This reference has already been disposed");
                    return this.target;
                }
            }

            public void Dispose()
            {
                if (!this.disposed)
                {
                    this.pool.Release(this.Target);
                    this.disposed = true;
                }
            }

            ~Ref()
            {
                Dispose();
            }
        }

        private bool open;
        private ReaderWriterLock rwlock;
        private ConcurrentQueue<T> pool;
        private PoolableObjectFactory<T> factory;

        public ObjectPool(PoolableObjectFactory<T> factory)
        {
            if (factory == null)
                throw new ArgumentNullException("factory");
            this.open = true;
            this.rwlock = new ReaderWriterLock();
            this.pool = new ConcurrentQueue<T>();
            this.factory = factory;
        }

        public Ref GetObject()
        {
            try
            {
                this.rwlock.AcquireReaderLock(-1);
                if (!this.open)
                    throw new InvalidOperationException("This pool is already closed");

                const int maxAttempts = 3;
                for (int attempt = 1 ; attempt <= maxAttempts ; attempt++)
                {
                    T item;
                    bool existing = this.pool.TryDequeue(out item);
                    if (!existing)
                    {
                        item = this.factory.Create();
                    }
                    if (validateBeforeReturn(item))
                    {
                        return new Ref(item, this);
                    }

                    // Bad item ... nuke it and try again (if still viable)
                    this.factory.Destroy(item);
                }
                throw new Exception($"Failed to create a new pooled object after {maxAttempts} attempts");
            }
            finally
            {
                this.rwlock.ReleaseReaderLock();
            }
        }

        private bool validateBeforeReturn(T item)
        {
            try
            {
                if (!this.factory.Validate(item)) return false;
                this.factory.Activate(item);
                return true;
            }
            catch (Exception)
            {
                // TODO: Log the failure somehow?
                return false;
            } 
        }

        private void Release(T item)
        {
            try
            {
                this.rwlock.AcquireReaderLock(-1);
                if (!this.open)
                    throw new InvalidOperationException("This pool is already closed");
                bool destroyItem = !this.factory.Validate(item);
                if (!destroyItem)
                {
                    try
                    {
                        this.factory.Passivate(item);
                        this.pool.Enqueue(item);
                    }
                    catch (Exception)
                    {
                        destroyItem = true;
                    }
                }
                if (destroyItem) this.factory.Destroy(item);
            }
            finally
            {
                this.rwlock.ReleaseReaderLock();
            }
        }

        public void Dispose()
        {
            try
            {
                this.rwlock.AcquireWriterLock(-1);
                if (!this.open) return;
                try
                {
                    foreach (T t in this.pool)
                    {
                        try
                        {
                            this.factory.Destroy(t);
                        }
                        catch (Exception e)
                        {
                            // Report it? Regardless, we must keep going
                            e.ToString();
                        }
                    }
                }
                finally
                {
                    this.open = false;
                }
            }
            finally
            {
                this.rwlock.ReleaseWriterLock();
            }
        }

        ~ObjectPool()
        {
            Dispose();
        }
    }
}