package com.armedia.caliente.engine.importer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.Tools;

public class TypeDescriptor {

	private static final Collection<String> NO_DECORATORS = null;

	private final CmfType type;
	private final String subtype;
	private final Set<String> decorators;

	public TypeDescriptor(CmfType type, String subtype) {
		this(type, subtype, TypeDescriptor.NO_DECORATORS);
	}

	public TypeDescriptor(CmfType type, String subtype, String[] decorators) {
		this(type, subtype, (decorators != null ? Arrays.asList(decorators) : null));
	}

	public TypeDescriptor(CmfType type, String subtype, Collection<String> decorators) {
		this.type = type;
		this.subtype = subtype;
		if ((decorators == null) || decorators.isEmpty()) {
			this.decorators = Collections.emptySet();
		} else {
			Set<String> s = new LinkedHashSet<>();
			for (String str : decorators) {
				if (!StringUtils.isEmpty(str)) {
					s.add(str);
				}
			}
			this.decorators = Tools.freezeSet(s);
		}
	}

	public CmfType getType() {
		return this.type;
	}

	public String getSubtype() {
		return this.subtype;
	}

	public Set<String> getDecorators() {
		return this.decorators;
	}

	@Override
	public String toString() {
		return String.format("TypeDescriptor [type=%s, subtype=%s, decorators=%s]", this.type, this.subtype,
			this.decorators);
	}
}