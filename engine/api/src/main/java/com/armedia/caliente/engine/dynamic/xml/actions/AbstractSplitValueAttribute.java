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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue.Type;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractSplitValueAttribute extends AbstractTransformValue {

	@XmlElement(name = "separator", required = true)
	protected Expression separator;

	@XmlAttribute(name = "keepEmpty", required = false)
	protected boolean keepEmpty = false;

	public Expression getSeparator() {
		return this.separator;
	}

	public void setSeparator(Expression separator) {
		this.separator = separator;
	}

	public boolean isKeepEmpty() {
		return this.keepEmpty;
	}

	public void setKeepEmpty(boolean keepEmpty) {
		this.keepEmpty = keepEmpty;
	}

	@Override
	protected DynamicValue executeAction(DynamicElementContext<?> ctx, DynamicValue candidate) throws ActionException {
		final String separator = Tools.toString(ActionTools.eval(getSeparator(), ctx));
		if (StringUtils.isEmpty(separator)) {
			final String lang = this.separator.getLang();
			throw new ActionException(String.format("This separator %s expression yielded an empty string: <%s>%s</%s>",
				lang, lang, this.separator.getScript(), lang));
		}
		final char sep = separator.charAt(0);
		Stream<String> s = candidate.getValues().stream()//
			.map(Tools::toString) //
			.flatMap((str) -> {
				Collection<String> c = Tools.splitEscaped(sep, str);
				if ((c == null) || c.isEmpty()) { return Stream.empty(); }
				return c.stream();
			});
		// Filter empty values
		if (!this.keepEmpty) {
			s = s.filter(StringUtils::isNotEmpty);
		}
		DynamicValue result = new DynamicValue(candidate.getName(), Type.STRING, true);
		result.setValues(s.collect(Collectors.toCollection(LinkedList::new)));
		return result;
	}

}