package com.armedia.cmf.engine.cmis.exporter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Property;

import com.armedia.cmf.engine.cmis.CmisTranslator;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public abstract class CmisObjectDelegate<T extends CmisObject> extends CmisExportDelegate<T> {

	protected CmisObjectDelegate(CmisExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		CmisTranslator translator = this.factory.getEngine().getTranslator();
		for (Property<?> prop : this.object.getProperties()) {
			CmfDataType t = CmisTranslator.decodePropertyType(prop.getType());
			CmfAttribute<CmfValue> att = new CmfAttribute<CmfValue>(prop.getId(), t, prop.isMultiValued());
			List<?> values = prop.getValues();
			List<CmfValue> l = new ArrayList<CmfValue>(values.size());
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
		return true;
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