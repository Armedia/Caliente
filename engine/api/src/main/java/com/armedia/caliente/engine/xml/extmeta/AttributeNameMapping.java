package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.engine.xml.Expression.ScriptContextConfig;
import com.armedia.caliente.engine.xml.ExpressionException;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataTransformNames.t", propOrder = {
	"mappings", "defaultTransform"
})
public class AttributeNameMapping {

	private static final boolean DEFAULT_CASE_SENSITIVE = true;

	@XmlElement(name = "map", required = false)
	protected List<MetadataNameMapping> mappings;

	@XmlElement(name = "default", required = false)
	protected Expression defaultTransform;

	@XmlTransient
	protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	protected Map<String, MetadataNameMapping> map = null;

	@XmlTransient
	protected Expression activeDefault = null;

	@XmlTransient
	protected Boolean caseSensitive = null;

	public List<MetadataNameMapping> getMappings() {
		if (this.mappings == null) {
			this.mappings = new ArrayList<>();
		}
		return this.mappings;
	}

	public void initialize(Boolean caseSensitive) {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (this.map != null) { return; }
			this.caseSensitive = Tools.coalesce(caseSensitive, AttributeNameMapping.DEFAULT_CASE_SENSITIVE);
			this.activeDefault = this.defaultTransform;
			Map<String, MetadataNameMapping> map = new HashMap<>();
			for (final MetadataNameMapping mapping : getMappings()) {
				String key = mapping.from;
				if (!this.caseSensitive) {
					key = StringUtils.upperCase(key);
				}
				if ((key != null) && (mapping.to != null)) {
					map.put(key, mapping);
				}
			}
			this.map = Tools.freezeMap(map);
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

	public Expression getDefaultTransform() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.defaultTransform;
		} finally {
			l.unlock();
		}
	}

	public void setDefaultTransform(Expression value) {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			this.defaultTransform = value;
			close();
			initialize(this.caseSensitive);
		} finally {
			l.unlock();
		}
	}

	protected boolean isSameName(String a, String b) {
		if (!this.caseSensitive) { return StringUtils.equalsIgnoreCase(a, b); }
		return Tools.equals(a, b);
	}

	public String transformName(final String sqlName) throws ExpressionException {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			String key = sqlName;
			if (!this.caseSensitive) {
				key = StringUtils.upperCase(key);
			}
			final MetadataNameMapping mapping = this.map.get(key);
			Expression exp = null;
			if (mapping == null) {
				// Use the default expression...
				exp = this.activeDefault;
			} else {
				exp = mapping.to;
			}

			// If there's no expression to evaluate, foxtrot oscar
			if (exp == null) { return sqlName; }

			// There's an expression to evaluate!! Let's do it!
			Object value = exp.evaluate(new ScriptContextConfig() {
				@Override
				public void configure(ScriptContext ctx) {
					final Bindings bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
					bindings.put("sqlName", sqlName);
					if (mapping != null) {
						bindings.put("mapName", mapping.from);
					}
				}
			});
			return Tools.toString(Tools.coalesce(value, sqlName));
		} finally {
			l.unlock();
		}
	}

	public void close() {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (this.map == null) { return; }
			this.activeDefault = null;
			this.caseSensitive = null;
			this.map = null;
		} finally {
			l.unlock();
		}
	}
}