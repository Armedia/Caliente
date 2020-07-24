using System;
using System.Collections.Concurrent;
using System.Threading;

namespace Armedia.CMSMF.SharePoint.Common
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
        private PoolableObjectFactory<T> generator;

        public ObjectPool(PoolableObjectFactory<T> generator)
        {
            if (generator == null)
                throw new ArgumentNullException("generator");
            this.open = true;
            this.rwlock = new ReaderWriterLock();
            this.pool = new ConcurrentQueue<T>();
            this.generator = generator;
        }

        public Ref GetObject()
        {
            try
            {
                this.rwlock.AcquireReaderLock(-1);
                if (!this.open)
                    throw new InvalidOperationException("This pool is already closed");
                bool createItem = false;
                T item;
                createItem = !this.pool.TryDequeue(out item);
                if (createItem)
                {
                    item = this.generator.Create();
                }
                try
                {
                    if (this.generator.Validate(item))
                    {
                        this.generator.Activate(item);
                    }
                }
                catch (Exception)
                {
                    this.generator.Destroy(item);
                    createItem = true;
                }
                return new Ref(item, this);
            }
            finally
            {
                this.rwlock.ReleaseReaderLock();
            }
        }

        private void Release(T item)
        {
            try
            {
                this.rwlock.AcquireReaderLock(-1);
                if (!this.open)
                    throw new InvalidOperationException("This pool is already closed");
                bool destroyItem = !this.generator.Validate(item);
                if (!destroyItem)
                {
                    try
                    {
                        this.generator.Passivate(item);
                        this.pool.Enqueue(item);
                    }
                    catch (Exception)
                    {
                        destroyItem = true;
                    }
                }
                if (destroyItem) this.generator.Destroy(item);
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
                            this.generator.Destroy(t);
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