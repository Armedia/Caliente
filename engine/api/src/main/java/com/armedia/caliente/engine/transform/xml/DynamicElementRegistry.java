package com.armedia.caliente.engine.transform.xml;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

public class DynamicElementRegistry<E> {

	private static class BasicElementFactory<E> implements DynamicElementFactory<E> {

		private final Class<E> klass;

		private BasicElementFactory(Class<E> klass) {
			this.klass = klass;
		}

		@Override
		public E acquireInstance() {
			try {
				return this.klass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeTransformationException(
					String.format("Failed to instantiate an item of class [%s]", this.klass.getCanonicalName()), e);
			}
		}

		@Override
		public void releaseInstance(E e) {
			// Do nothing...
		}
	}

	private final ConcurrentMap<String, DynamicElementFactory<E>> factories = new ConcurrentHashMap<>();

	private final Class<E> elementClass;

	public DynamicElementRegistry(Class<E> elementClass) {
		this.elementClass = elementClass;
	}

	public <V> DynamicElementFactory<E> getFactory(final String className) throws Exception {

		final Class<?> c = Class.forName(className);
		if (this.elementClass.isAssignableFrom(c)) {
			// It's a condition, so try to get an instance right off the bat!
			return ConcurrentUtils.createIfAbsent(this.factories, className,
				new ConcurrentInitializer<DynamicElementFactory<E>>() {
					@Override
					public DynamicElementFactory<E> get() throws ConcurrentException {
						// Get a new, constant instance
						@SuppressWarnings("unchecked")
						Class<E> k = (Class<E>) c;
						return new BasicElementFactory<>(k);
					}
				});
		}

		// Not an element itself, but actually a factory...so...register it!
		if (DynamicElementFactory.class.isAssignableFrom(c)) {
			// It's a factory, so we use it to get an instance
			return ConcurrentUtils.createIfAbsent(this.factories, className,
				new ConcurrentInitializer<DynamicElementFactory<E>>() {

					@Override
					public DynamicElementFactory<E> get() throws ConcurrentException {
						try {
							@SuppressWarnings("unchecked")
							Class<DynamicElementFactory<E>> k = (Class<DynamicElementFactory<E>>) c;
							return k.newInstance();
						} catch (InstantiationException | IllegalAccessException e) {
							throw new ConcurrentException(e);
						}
					}
				});
		}

		// If it's not a factory nor a condition, we don't know what to do with it...
		throw new RuntimeTransformationException(
			String.format("The class [%s] is neither a Action nor a TransformationFactory", className));
	}

}