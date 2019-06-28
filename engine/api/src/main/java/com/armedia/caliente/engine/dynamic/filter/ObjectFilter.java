/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dynamic.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DefaultDynamicObject;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.ProcessingCompletedException;
import com.armedia.caliente.engine.dynamic.xml.FilterOutcome;
import com.armedia.caliente.engine.dynamic.xml.Filters;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;
import com.armedia.caliente.engine.dynamic.xml.filter.Filter;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

public class ObjectFilter extends BaseShareableLockable {

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
	private final List<Filter> activeFilters = new ArrayList<>();
	private final FilterOutcome defaultOutcome;
	private boolean closed = false;

	private ObjectFilter(Filters filters) {
		this.activeFilters.addAll(filters.getFilters());
		this.defaultOutcome = Tools.coalesce(filters.getDefaultOutcome(), FilterOutcome.ACCEPT);
	}

	public Boolean accept(CmfObject<CmfValue> cmfObject, CmfValueMapper mapper) throws ObjectFilterException {
		Objects.requireNonNull(cmfObject, "Must provide an object to filter");
		try (SharedAutoLock lock = autoSharedLock()) {
			if (this.closed) { throw new ObjectFilterException("This object filter is already closed"); }
			DynamicElementContext ctx = new DynamicElementContext(cmfObject, new DefaultDynamicObject(cmfObject),
				mapper, null);
			for (Filter f : this.activeFilters) {
				try {
					f.apply(ctx);
				} catch (ProcessingCompletedException e) {
					// The object was explicitly accepted!
					this.log.trace("Filter logic accepted {}", cmfObject.getDescription());
					return true;
				} catch (ObjectRejectedByFilterException e) {
					// The object was explicitly filtered!
					this.log.info("Filter logic rejected {}", cmfObject.getDescription());
					return false;
				} catch (ActionException e) {
					this.log.trace("Exception caught while processing filters for {}", cmfObject.getDescription(), e);
					throw new ObjectFilterException(e);
				}
			}
			boolean ret = false;
			switch (this.defaultOutcome) {
				case ACCEPT:
					ret = true;
					break;
				case REJECT:
					ret = false;
					break;
			}
			this.log.trace("Default action: {} {}", StringUtils.capitalize(this.defaultOutcome.name().toLowerCase()),
				cmfObject.getDescription());
			return ret;
		}
	}

	public <V> boolean acceptRaw(CmfObject<V> object, CmfValueMapper mapper) throws ObjectFilterException {
		Objects.requireNonNull(object, "Must provide an object to filter");
		return accept(object.getTranslator().encodeObject(object), mapper);
	}

	public void close() {
		try (MutexAutoLock lock = autoMutexLock()) {
			try {
				if (this.closed) { return; }
				this.activeFilters.clear();
			} finally {
				this.closed = true;
			}
		}
	}
}