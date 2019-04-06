package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.AutoLock;
import com.armedia.commons.utilities.concurrent.ShareableLockable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataTransformNames.t", propOrder = {
	"mappings", "defaultTransform"
})
public class AttributeNameMapping implements ShareableLockable {

	private static final boolean DEFAULT_CASE_SENSITIVE = true;

	private static class NameMatcher {
		private final Pattern pattern;
		private final Expression expression;

		private NameMatcher(Pattern pattern, Expression expression) {
			this.pattern = pattern;
			this.expression = expression;
		}
	}

	@XmlElement(name = "map", required = false)
	protected List<MetadataNameMapping> mappings;

	@XmlElement(name = "default", required = false)
	protected Expression defaultTransform;

	@XmlTransient
	protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	protected List<NameMatcher> matchers = null;

	@XmlTransient
	protected Expression activeDefault = null;

	@XmlTransient
	protected Boolean caseSensitive = null;

	@Override
	public final ReadWriteLock getShareableLock() {
		return this.rwLock;
	}

	public List<MetadataNameMapping> getMappings() {
		if (this.mappings == null) {
			this.mappings = new ArrayList<>();
		}
		return this.mappings;
	}

	public void initialize(Boolean caseSensitive) {
		shareLockedUpgradable(() -> this.matchers, Objects::isNull, (e) -> {
			this.caseSensitive = Tools.coalesce(caseSensitive, AttributeNameMapping.DEFAULT_CASE_SENSITIVE);
			this.activeDefault = this.defaultTransform;
			List<NameMatcher> matchers = new ArrayList<>();
			for (final MetadataNameMapping mapping : getMappings()) {
				if ((mapping.from != null) && (mapping.to != null)) {
					Pattern p = Pattern.compile(mapping.from, this.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
					matchers.add(new NameMatcher(p, mapping.to));
				}
			}
			this.matchers = Tools.freezeList(matchers);
		});
	}

	public boolean isCaseSensitive() {
		return shareLocked(() -> this.caseSensitive);
	}

	public Expression getDefaultTransform() {
		return shareLocked(() -> this.defaultTransform);
	}

	public void setDefaultTransform(Expression value) {
		try (AutoLock lock = autoMutexLock()) {
			this.defaultTransform = value;
			close();
			initialize(this.caseSensitive);
		}
	}

	public String transformName(final String sqlName) throws ScriptException {
		try (AutoLock lock = autoSharedLock()) {
			boolean hasGroups = false;
			String regex = null;
			Expression repl = null;
			for (NameMatcher nm : this.matchers) {
				Matcher m = nm.pattern.matcher(sqlName);
				if (m.matches()) {
					regex = nm.pattern.pattern();
					repl = nm.expression;
					hasGroups = (m.groupCount() > 0);
					break;
				}
			}

			// If we have no match yet, then we go with the default
			if (repl == null) {
				repl = this.activeDefault;
			}

			String value = Tools
				.toString(repl.evaluate((ctx) -> ctx.getBindings(ScriptContext.ENGINE_SCOPE).put("sqlName", sqlName)));

			// If there is no replacement, then we simply return the original name
			if (value == null) { return sqlName; }

			// If there is a replacement, we check to see if we have to return it verbatim or not.
			// We return verbatim when there's no regex (i.e. this is the default value), or when
			// the matching regex has no capturing groups (i.e. it's a literal)
			if ((regex == null) || !hasGroups) { return value; }

			// We have a regular expression, a replacement, and the regex has capturing groups, so
			// we have to do a replacement
			return sqlName.replaceAll(regex, value);
		}
	}

	public void close() {
		try (AutoLock lock = autoMutexLock()) {
			if (this.matchers == null) { return; }
			this.activeDefault = null;
			this.caseSensitive = null;
			this.matchers = null;
		}
	}
}