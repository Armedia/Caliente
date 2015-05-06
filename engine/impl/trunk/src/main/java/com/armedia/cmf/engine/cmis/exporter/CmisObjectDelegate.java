package com.armedia.cmf.engine.cmis.exporter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Property;

import com.armedia.cmf.engine.cmis.CmisTranslator;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;

public abstract class CmisObjectDelegate<T extends CmisObject> extends CmisExportDelegate<T> {

	protected CmisObjectDelegate(CmisExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
	}

	@Override
	protected void marshal(CmisExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		CmisTranslator translator = this.factory.getEngine().getTranslator();
		for (Property<?> prop : this.object.getProperties()) {
			StoredDataType t = CmisTranslator.decodePropertyType(prop.getType());
			StoredAttribute<StoredValue> att = new StoredAttribute<StoredValue>(prop.getId(), t, prop.isMultiValued());
			List<?> values = prop.getValues();
			List<StoredValue> l = new ArrayList<StoredValue>(values.size());
			int i = 0;
			for (Object v : prop.getValues()) {
				try {
					l.add(translator.getValue(t, v));
					i++;
				} catch (ParseException e) {
					throw new ExportException(String.format(
						"Failed to encode value #%d for %s (%s) property [%s] for %s with ID [%s]: [%s]", i,
						att.getType(), prop.getType(), prop.getId(), object.getType(), object.getId(), v), e);
				}
			}
			att.setValues(l);
			object.setAttribute(att);
		}
	}

	@Override
	protected String calculateLabel(T obj) throws Exception {
		CmisObject o = CmisObject.class.cast(obj);
		return String.format("[%s|%s]", o.getType().getId(), o.getName());
	}

	@Override
	protected final String calculateObjectId(T object) throws Exception {
		return object.getId();
	}

	@Override
	protected final String calculateSearchKey(T object) throws Exception {
		return object.getId();
	}
}