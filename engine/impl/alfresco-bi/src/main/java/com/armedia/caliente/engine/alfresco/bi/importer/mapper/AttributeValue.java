package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class AttributeValue implements Iterable<CmfValue> {
	public static final char DEFAULT_SEPARATOR = ',';

	private final String sourceName;
	private final String targetName;
	private final Collection<CmfValue> values;
	private final boolean override;
	private final char separator;

	AttributeValue(CmfAttribute<CmfValue> sourceAttribute, String targetName, char separator, boolean override) {
		this.sourceName = sourceAttribute.getName();
		this.targetName = targetName;
		this.values = Tools.freezeCopy(new ArrayList<>(sourceAttribute.getValues()));
		this.override = override;
		this.separator = separator;
	}

	AttributeValue(String targetName, char separator, boolean override, Collection<CmfValue> values) {
		this.sourceName = null;
		this.targetName = targetName;
		this.values = Tools.freezeList(new ArrayList<>(values));
		this.override = override;
		this.separator = separator;
	}

	AttributeValue(String targetName, char separator, boolean override, CmfValue... values) {
		this(targetName, separator, override, Arrays.asList(values));
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public String getTargetName() {
		return this.targetName;
	}

	public Collection<CmfValue> getValues() {
		return this.values;
	}

	public char getSeparator() {
		return this.separator;
	}

	public boolean isOverride() {
		return this.override;
	}

	@Override
	public Iterator<CmfValue> iterator() {
		return this.values.iterator();
	}

	@Override
	public String toString() {
		return Tools.toString(this.values);
	}
}