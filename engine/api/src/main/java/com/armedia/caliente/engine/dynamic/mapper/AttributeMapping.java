package com.armedia.caliente.engine.dynamic.mapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

public class AttributeMapping implements Iterable<Object> {
	public static final char DEFAULT_SEPARATOR = ',';

	private final String sourceName;
	private final String targetName;
	private final Collection<Object> values;
	private final boolean override;
	private final char separator;
	private final CmfDataType type;
	private final boolean repeating;

	AttributeMapping(DynamicValue sourceAttribute, String targetName, char separator, boolean override) {
		this.sourceName = sourceAttribute.getName();
		this.targetName = targetName;
		this.values = sourceAttribute.getValues();
		this.override = override;
		this.separator = separator;
		this.type = sourceAttribute.getType();
		this.repeating = sourceAttribute.isRepeating();
	}

	AttributeMapping(String targetName, char separator, boolean override, CmfDataType type, Collection<Object> values) {
		this.sourceName = null;
		this.targetName = targetName;
		this.values = Tools.coalesce(values, Collections.emptyList());
		this.override = override;
		this.separator = separator;
		this.repeating = (values.size() > 1);
		this.type = type;
	}

	AttributeMapping(String targetName, char separator, boolean override, CmfDataType type, Object... values) {
		this(targetName, separator, override, type, Arrays.asList(values));
	}

	public CmfDataType getType() {
		return this.type;
	}

	public boolean isRepeating() {
		return this.repeating;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public String getTargetName() {
		return this.targetName;
	}

	public Collection<Object> getValues() {
		return this.values;
	}

	public char getSeparator() {
		return this.separator;
	}

	public boolean isOverride() {
		return this.override;
	}

	public int getValueCount() {
		return this.values.size();
	}

	@Override
	public Iterator<Object> iterator() {
		return this.values.iterator();
	}

	@Override
	public String toString() {
		return Tools.toString(this.values);
	}
}