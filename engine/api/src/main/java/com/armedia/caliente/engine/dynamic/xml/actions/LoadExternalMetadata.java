
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataException;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionLoadExternalMetadata.t", propOrder = {
	"sources"
})
public class LoadExternalMetadata extends ConditionalAction {

	@XmlElement(name = "metadataSet", required = false)
	protected List<ExternalMetadataSet> sources;

	public List<ExternalMetadataSet> getSources() {
		if (this.sources == null) {
			this.sources = new ArrayList<>();
		}
		return this.sources;
	}

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		for (ExternalMetadataSet metadataSource : getSources()) {
			if (metadataSource == null) {
				continue;
			}

			String sourceName;
			try {
				sourceName = Tools.toString(Expression.eval(metadataSource, ctx));
			} catch (ScriptException e) {
				throw new ActionException(e);
			}

			if (StringUtils.isEmpty(sourceName)) {
				continue;
			}

			final boolean override = metadataSource.isOverride();
			final Map<String, CmfAttribute<CmfValue>> externalAttributes;
			try {
				externalAttributes = ctx.getAttributeValues(ctx.getBaseObject(), sourceName);
			} catch (ExternalMetadataException e) {
				throw new ActionException(String.format("Failed to load the external metadata for %s from source [%s]",
					ctx.getBaseObject().getDescription(), sourceName), e);
			}

			Map<String, DynamicValue> currentAttributes = ctx.getDynamicObject().getAtt();
			for (String attributeName : externalAttributes.keySet()) {
				if (override || !currentAttributes.containsKey(attributeName)) {
					final CmfAttribute<CmfValue> external = externalAttributes.get(attributeName);
					final DynamicValue newAttribute = new DynamicValue(external);
					currentAttributes.put(attributeName, newAttribute);
					final List<Object> newValues = new ArrayList<>(external.getValueCount());
					for (CmfValue v : external) {
						newValues.add(v.asObject());
					}
					newAttribute.setValues(newValues);
				}
			}
		}
	}

}