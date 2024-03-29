/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataTransformNames.t", propOrder = {
	"mappings", "defaultTransform"
})
public class AttributeNameMapping extends BaseShareableLockable {

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
	protected List<NameMatcher> matchers = null;

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
		try (MutexAutoLock lock = mutexAutoLock()) {
			this.defaultTransform = value;
			close();
			initialize(this.caseSensitive);
		}
	}

	public String transformName(final String sqlName) throws ScriptException {
		try (SharedAutoLock lock = sharedAutoLock()) {
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
		try (MutexAutoLock lock = mutexAutoLock()) {
			if (this.matchers == null) { return; }
			this.activeDefault = null;
			this.caseSensitive = null;
			this.matchers = null;
		}
	}
}