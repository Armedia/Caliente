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
package com.armedia.caliente.engine.cmis.exporter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.cmis.CmisTranslator;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public abstract class CmisObjectDelegate<T extends CmisObject> extends CmisExportDelegate<T> {

	protected CmisObjectDelegate(CmisExportDelegateFactory factory, Session session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		CmisTranslator translator = this.factory.getEngine().getTranslator();
		for (Property<?> prop : this.object.getProperties()) {
			CmfValue.Type t = CmisTranslator.decodePropertyType(prop.getType());
			CmfAttribute<CmfValue> att = new CmfAttribute<>(prop.getId(), t, prop.isMultiValued());
			List<?> values = prop.getValues();
			List<CmfValue> l = new ArrayList<>(values.size());
			int i = 0;
			for (Object v : prop.getValues()) {
				try {
					l.add(translator.getValue(t, v));
					i++;
				} catch (ParseException e) {
					throw new ExportException(
						String.format("Failed to encode value #%d for %s (%s) property [%s] for %s with ID [%s]: [%s]",
							i, att.getType(), prop.getType(), prop.getId(), object.getType(), object.getId(), v),
						e);
				}
			}
			att.setValues(l);
			object.setAttribute(att);
		}
		return true;
	}

	@Override
	protected String calculateLabel(Session session, T obj) throws Exception {
		CmisObject o = CmisObject.class.cast(obj);
		return String.format("[%s|%s]", o.getType().getId(), o.getName());
	}

	@Override
	protected String calculateHistoryId(Session session, T object) throws Exception {
		return object.getId();
	}

	@Override
	protected final String calculateObjectId(Session session, T object) throws Exception {
		return object.getId();
	}

	@Override
	protected final String calculateSearchKey(Session session, T object) throws Exception {
		return object.getId();
	}

	@Override
	protected int calculateDependencyTier(Session session, T object) throws Exception {
		return 0;
	}
}