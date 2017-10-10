
package com.armedia.caliente.engine.xml.actions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.extmeta.ExternalMetadataException;
import com.armedia.caliente.engine.extmeta.ExternalMetadataLoader;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
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
		ExternalMetadataLoader loader = null;
		Set<String> names = new LinkedHashSet<>();
		for (Expression source : getSources()) {
			String name;
			try {
				name = Tools.toString(Expression.eval(source, ctx));
			} catch (ScriptException e) {
				throw new TransformationException(e);
			}
			if (!StringUtils.isEmpty(name)) {
				names.add(name);
			}
		}
		if (!names.isEmpty()) {
			try {
				@SuppressWarnings("null")
				Map<String, CmfAttribute<CmfValue>> attributes = loader.getAttributeValues(ctx.getBaseObject(), names);
				for (String name : attributes.keySet()) {
					attributes.get(name).hashCode();
				}
			} catch (ExternalMetadataException e) {
				throw new TransformationException("Failed to load the external metadata for ...", e);
			}
		}
	}

}