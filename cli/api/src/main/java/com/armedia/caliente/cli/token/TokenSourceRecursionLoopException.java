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
package com.armedia.caliente.cli.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public class TokenSourceRecursionLoopException extends TokenLoaderException {
	private static final long serialVersionUID = 1L;

	private final TokenSource loopedSource;
	private final List<String> sources;

	public TokenSourceRecursionLoopException(TokenSource loopedSource, Collection<String> sources) {
		super(String.format("Token source recursion loop: [%s] is already visited as per %s", loopedSource.getKey(),
			sources));
		this.loopedSource = loopedSource;
		this.sources = Tools.freezeList(new ArrayList<>(sources));
	}

	public final TokenSource getLoopedURL() {
		return this.loopedSource;
	}

	public final List<String> getSources() {
		return this.sources;
	}
}