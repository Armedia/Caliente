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

package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractSplitJoinValueAttribute extends AbstractTransformValue {

	@XmlElement(name = "separator", required = true)
	protected Expression separator;

	@XmlElement(name = "keepEmpty", required = false)
	protected Expression keepEmpty;

	public final Expression getSeparator() {
		return this.separator;
	}

	public final void setSeparator(Expression separator) {
		this.separator = separator;
	}

	public final Expression getKeepEmpty() {
		return this.keepEmpty;
	}

	public final void setKeepEmpty(Expression keepEmpty) {
		this.keepEmpty = keepEmpty;
	}

	protected final boolean isKeepEmpty(DynamicElementContext<?> ctx) throws ActionException {
		return Tools.decodeBoolean(ActionTools.eval(getKeepEmpty(), ctx), false);
	}

	protected abstract DynamicValue processValues(DynamicElementContext<?> ctx, DynamicValue candidate,
		Collection<String> strings, char sep) throws ActionException;

	protected Stream<String> prepareValueStream(Stream<String> s, char sep) {
		return s;
	}

	@Override
	protected final DynamicValue executeAction(DynamicElementContext<?> ctx, DynamicValue candidate)
		throws ActionException {
		final String separator = Tools.toString(ActionTools.eval(getSeparator(), ctx));
		if (StringUtils.isEmpty(separator)) {
			final String lang = this.separator.getLang();
			throw new ActionException(String.format("This separator %s expression yielded an empty string: <%s>%s</%s>",
				lang, lang, this.separator.getScript(), lang));
		}
		final char sep = separator.charAt(0);
		Stream<String> s = candidate.getValues().stream() //
			.map(Tools::toString) //
		;
		s = prepareValueStream(s, sep);
		// Filter empty values
		if (!isKeepEmpty(ctx)) {
			s = s.filter(StringUtils::isNotEmpty);
		}
		return processValues(ctx, candidate, s.collect(Collectors.toCollection(LinkedList::new)), sep);
	}
}