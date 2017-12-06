
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
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionLoadExternalMetadata.t", propOrder = {
	"metadataSets"
})
public class LoadExternalMetadata extends ConditionalAction {

	@XmlElement(name = "metadata-set", required = false)
	protected List<ExternalMetadataSet> metadataSets;

	public List<ExternalMetadataSet> getMetadataSets() {
		if (this.metadataSets == null) {
			this.metadataSets = new ArrayList<>();
		}
		return this.metadataSets;
	}

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		for (ExternalMetadataSet metadataSource : getMetadataSets()) {
			if (metadataSource == null) {
				continue;
			}

			String setName;
			try {
				setName = Tools.toString(Expression.eval(metadataSource, ctx));
			} catch (ScriptException e) {
				throw new ActionException(e);
			}

			if (StringUtils.isEmpty(setName)) {
				continue;
			}

			final boolean override = metadataSource.isOverride();
			final Map<String, CmfAttribute<CmfValue>> externalAttributes;
			try {
				externalAttributes = ctx.getAttributeValues(ctx.getBaseObject(), setName);
			} catch (ExternalMetadataException e) {
				throw new ActionException(
					String.format("Failed to load the external metadata for %s from metadata set [%s]",
						ctx.getBaseObject().getDescription(), setName),
					e);
			}

			final String varName = String.format("emdl:%s", setName);
			ctx.getVariables().put(varName,
				new DynamicValue(varName, CmfDataType.BOOLEAN, false).setValue(externalAttributes != null));
			if (externalAttributes == null) {
				// Nothing was loaded...
				continue;
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