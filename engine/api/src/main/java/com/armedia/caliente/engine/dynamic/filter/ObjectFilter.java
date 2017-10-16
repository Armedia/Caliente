package com.armedia.caliente.engine.dynamic.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DefaultDynamicObject;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.ProcessingCompletedException;
import com.armedia.caliente.engine.dynamic.xml.Filters;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;
import com.armedia.caliente.engine.dynamic.xml.filter.Filter;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;

public class ObjectFilter {

	private static final XmlInstances<Filters> INSTANCES = new XmlInstances<>(Filters.class);

	public static ObjectFilter getObjectFilter(String location, boolean failIfMissing) throws ObjectFilterException {
		try {
			try {
				Filters filters = ObjectFilter.INSTANCES.getInstance(location);
				if (filters == null) { return null; }
				return new ObjectFilter(filters);
			} catch (final XmlNotFoundException e) {
				if (!failIfMissing) { return null; }
				throw e;
			}
		} catch (Exception e) {
			String pre = "";
			String post = "";
			if (location == null) {
				pre = "default ";
			} else {
				post = String.format(" from [%s]", location);
			}
			throw new ObjectFilterException(
				String.format("Failed to load the %sexternal metadata configuration%s", pre, post), e);
		}
	}

	public static String getDefaultLocation() {
		return ObjectFilter.INSTANCES.getDefaultFileName();
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	private boolean initialized = false;

	private final Filters filters;
	private final List<Filter> activeFilters = new ArrayList<>();

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	private ObjectFilter(Filters filters) {
		this.filters = filters;
	}

	public void initialize() throws ObjectFilterException {
		final Lock w = this.rwLock.writeLock();
		w.lock();
		try {
			if (this.initialized) { return; }
			this.activeFilters.addAll(this.filters.getFilters());
			this.initialized = true;
		} finally {
			w.unlock();
		}
	}

	public Boolean accept(CmfObject<CmfValue> cmfObject, CmfValueMapper mapper) throws ObjectFilterException {
		Objects.requireNonNull(cmfObject, "Must provide an object to filter");
		DynamicElementContext ctx = new DynamicElementContext(cmfObject, new DefaultDynamicObject(cmfObject), mapper,
			null);
		for (Filter f : this.activeFilters) {
			try {
				f.apply(ctx);
			} catch (ProcessingCompletedException e) {
				// The object was explicitly accepted!
				break;
			} catch (ObjectRejectedByFilterException e) {
				// The object was explicitly filtered!
				this.log.info("Explicitly filtered {}", cmfObject.getDescription());
				return false;
			} catch (ActionException e) {
				this.log.trace("Exception caught while processing filters for {}", cmfObject.getDescription(), e);
				throw new ObjectFilterException(e);
			}
		}
		this.log.trace("Accepting {}", cmfObject.getDescription());
		return true;
	}

	public <V> boolean acceptRaw(CmfObject<V> object, CmfValueMapper mapper) throws ObjectFilterException {
		Objects.requireNonNull(object, "Must provide an object to filter");
		return accept(object.getTranslator().encodeObject(object), mapper);
	}

	public void close() {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (!this.initialized) { return; }
			try {
				this.activeFilters.clear();
			} finally {
				this.initialized = false;
			}
		} finally {
			l.unlock();
		}
	}
}