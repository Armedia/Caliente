
package com.armedia.caliente.engine.xml.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.extmeta.ExternalMetadataException;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.xml.ConditionalAction;
import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionLoadExternalMetadata.t", propOrder = {
	"sources"
})
public class LoadExternalMetadata extends ConditionalAction {

	@XmlElement(name = "from", required = false)
	protected List<Expression> sources;

	public List<Expression> getSources() {
		if (this.sources == null) {
			this.sources = new ArrayList<>();
		}
		return this.sources;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		for (Expression metadataSource : getSources()) {
			String sourceName;
			try {
				sourceName = Tools.toString(Expression.eval(metadataSource, ctx));
			} catch (ScriptException e) {
				throw new TransformationException(e);
			}

			if (StringUtils.isEmpty(sourceName)) {
				continue;
			}

			boolean override = false;

			final Map<String, CmfAttribute<CmfValue>> externalAttributes;
			try {
				externalAttributes = ctx.getAttributeValues(ctx.getBaseObject(), sourceName);
			} catch (ExternalMetadataException e) {
				throw new TransformationException(
					String.format("Failed to load the external metadata for %s from source [%s]",
						ctx.getBaseObject().getDescription(), sourceName),
					e);
			}

			Map<String, TypedValue> currentAttributes = ctx.getTransformableObject().getAtt();
			for (String attributeName : externalAttributes.keySet()) {
				if (override || !currentAttributes.containsKey(attributeName)) {
					final CmfAttribute<CmfValue> external = externalAttributes.get(attributeName);
					final TypedValue newAttribute = new TypedValue(external);
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