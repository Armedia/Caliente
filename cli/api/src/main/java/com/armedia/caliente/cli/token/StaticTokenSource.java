/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.armedia.commons.utilities.Tools;

public class StaticTokenSource implements TokenSource {

	private static final AtomicLong counter = new AtomicLong(0);

	private final String key;
	private final List<String> tokens;

	public StaticTokenSource() {
		this(null, null);
	}

	public StaticTokenSource(String key) {
		this(key, null);
	}

	public StaticTokenSource(Collection<String> tokens) {
		this(null, tokens);
	}

	public StaticTokenSource(String key, Collection<String> tokens) {
		if (key == null) {
			key = String.format("(static-%016X)", StaticTokenSource.counter.getAndIncrement());
		}
		this.key = key;
		this.tokens = Tools.freezeList(new ArrayList<>(tokens));
	}

	@Override
	public List<String> getTokenStrings() throws IOException {
		return this.tokens;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public String toString() {
		return String.format("StaticTokenSource [key=%s]", this.key);
	}
}