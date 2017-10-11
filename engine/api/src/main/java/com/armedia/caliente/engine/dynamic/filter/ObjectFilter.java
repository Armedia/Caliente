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
import com.armedia.caliente.engine.dynamic.xml.XmlInstanceException;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.filter.Filter;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;

public class ObjectFilter {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final XmlInstances<Filters> INSTANCES = new XmlInstances<>(Filters.class);

	private boolean initialized = false;

	private final String locationDesc;
	private final Filters filters;
	private final List<Filter> activeFilters = new ArrayList<>();

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	public ObjectFilter(String location) throws FilterException {
		if (location == null) {
			this.locationDesc = "the default configuration";
		} else {
			this.locationDesc = String.format("configuration [%s]", location);
		}
		try {
			this.filters = ObjectFilter.INSTANCES.getInstance(location);
		} catch (XmlInstanceException e) {
			throw new FilterException(
				String.format("Failed to load the external metadata configuration from %s", this.locationDesc), e);
		}
	}

	public void initialize() throws FilterException {
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

	public <V> boolean accept(CmfObject<V> object, CmfValueMapper mapper) throws FilterException {
		Objects.requireNonNull(object, "Must provide an object to filter");
		CmfObject<CmfValue> cmfObject = object.getTranslator().encodeObject(object);
		DynamicElementContext ctx = new DynamicElementContext(cmfObject, new DefaultDynamicObject(cmfObject), mapper,
			null);
		for (Filter f : this.activeFilters) {
			try {
				f.apply(ctx);
			} catch (ProcessingCompletedException e) {
				// The object was explicitly accepted!
				break;
			} catch (ObjectFilteredException e) {
				// The object was explicitly filtered!
				this.log.info("Explicitly filtered {}", object.getDescription());
				return false;
			} catch (ActionException e) {
				this.log.trace("Explicitly failed {}", object.getDescription(), e);
				throw new FilterException(e);
			}
		}
		this.log.trace("Accepting {}", object.getDescription());
		return true;
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