package com.armedia.caliente.engine.dynamic.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.xml.mapper.SetValue;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

class ConstantRenderer extends AttributeRenderer {

	private final Collection<AttributeMapping> value;

	public ConstantRenderer(SetValue m, Character parentSeparator) {
		super(m, parentSeparator);
		List<CmfValue> values = new ArrayList<>();
		final CmfDataType dataType = m.getType();
		Tools.splitEscaped(this.separator, m.getValue()).forEach((v) -> {
			try {
				values.add(dataType.getSerializer().deserialize(v));
			} catch (Exception e) {
				throw new RuntimeException(String.format(
					"Failed to deserialize the value [%s] as a %s for the constant mapping named [%s] (separator = [%s], full value = [%s])",
					v, dataType.name(), m.getTgt(), this.separator, m.getValue()));
			}
		});
		this.value = Collections
			.singleton(new AttributeMapping(m.getTgt(), this.separator, m.isOverride(), dataType, values));
	}

	@Override
	public Collection<AttributeMapping> render(DynamicObject object, ResidualsModeTracker tracker) {
		return this.value;
	}
}