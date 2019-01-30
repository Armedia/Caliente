package com.armedia.caliente.store;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class CmfBaseSetting implements CmfSetting, Comparable<CmfBaseSetting> {

	protected final String name;
	protected final CmfDataType type;
	protected final boolean repeating;

	protected CmfBaseSetting(CmfBaseSetting pattern) {
		if (pattern == null) {
			throw new IllegalArgumentException("Must provide a non-null pattern to copy values from");
		}
		this.name = pattern.name;
		this.type = pattern.type;
		this.repeating = pattern.repeating;
	}

	public CmfBaseSetting(String name, CmfDataType type) {
		this(name, type, false);
	}

	public CmfBaseSetting(String name, CmfDataType type, boolean multiple) {
		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("Must provide a non-null, non-blank name");
		}
		if (type == null) { throw new IllegalArgumentException("Must provide a data type"); }
		this.type = type;
		this.name = name;
		this.repeating = multiple;
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final CmfDataType getType() {
		return this.type;
	}

	@Override
	public final boolean isRepeating() {
		return this.repeating;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.type, this.repeating);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfBaseSetting other = CmfBaseSetting.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.repeating != other.repeating) { return false; }
		return true;
	}

	@Override
	public int compareTo(CmfBaseSetting o) {
		if (o == null) { return 1; }
		int r = this.name.compareTo(o.name);
		if (r != 0) { return r; }
		r = this.type.compareTo(o.type);
		if (r != 0) { return r; }
		r = Tools.compare(this.repeating, o.repeating);
		if (r != 0) { return r; }
		return 0;
	}
}