package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.xml.CmfValueTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataAttributeTypes.t", propOrder = {
	"mappings", "defaultType"
})
public class AttributeTypeMapping {

	private static final boolean DEFAULT_CASE_SENSITIVE = true;

	private static class TypeMatcher {
		private final Pattern pattern;
		private final CmfValue.Type type;

		private TypeMatcher(Pattern pattern, CmfValue.Type type) {
			this.pattern = pattern;
			this.type = type;
		}
	}

	@XmlElement(name = "attribute", required = false)
	protected List<MetadataTypeMapping> mappings;

	@XmlElement(name = "default", required = false)
	@XmlJavaTypeAdapter(CmfValueTypeAdapter.class)
	protected CmfValue.Type defaultType;

	@XmlTransient
	protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	protected List<TypeMatcher> matchers = null;

	@XmlTransient
	protected CmfValue.Type activeDefault = null;

	@XmlTransient
	protected Boolean caseSensitive = null;

	public List<MetadataTypeMapping> getMappings() {
		if (this.mappings == null) {
			this.mappings = new ArrayList<>();
		}
		return this.mappings;
	}

	public void initialize(Boolean caseSensitive) {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (this.matchers != null) { return; }
			this.caseSensitive = Tools.coalesce(caseSensitive, AttributeTypeMapping.DEFAULT_CASE_SENSITIVE);

			this.activeDefault = this.defaultType;
			List<TypeMatcher> matchers = new ArrayList<>();
			for (final MetadataTypeMapping mapping : getMappings()) {
				String key = mapping.name;
				if ((key != null) && (mapping.type != null)) {
					Pattern p = Pattern.compile(mapping.name, this.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
					matchers.add(new TypeMatcher(p, mapping.type));
				}
			}
			this.matchers = Tools.freezeList(matchers);
		} finally {
			l.unlock();
		}
	}

	public boolean isCaseSensitive() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.caseSensitive;
		} finally {
			l.unlock();
		}
	}

	public CmfValue.Type getDefaultType() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.defaultType;
		} finally {
			l.unlock();
		}
	}

	public void setDefaultType(CmfValue.Type value) {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			this.defaultType = value;
			close();
			initialize(this.caseSensitive);
		} finally {
			l.unlock();
		}
	}

	public CmfValue.Type getMappedType(final String sqlName) {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			for (TypeMatcher tm : this.matchers) {
				if (tm.pattern.matcher(sqlName).matches()) { return tm.type; }
			}
			return this.activeDefault;
		} finally {
			l.unlock();
		}
	}

	public void close() {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (this.matchers == null) { return; }
			this.activeDefault = null;
			this.caseSensitive = null;
			this.matchers = null;
		} finally {
			l.unlock();
		}
	}
}