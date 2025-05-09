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
package com.armedia.caliente.engine.cmis;

import java.util.Iterator;

import com.armedia.commons.utilities.function.CheckedFunction;

public final class CmisTransformerIterator<S, T> implements Iterator<T> {

	private final Iterator<S> it;
	private final CheckedFunction<S, T, ? extends Exception> transformer;
	private long current = 0;

	public CmisTransformerIterator(Iterator<S> results, CheckedFunction<S, T, ? extends Exception> transformer) {
		if (transformer == null) { throw new IllegalArgumentException("Must provide a transformer"); }
		this.transformer = transformer;
		this.it = results;
	}

	@Override
	public boolean hasNext() {
		return this.it.hasNext();
	}

	@Override
	public T next() {
		try {
			T result = this.transformer.applyChecked(this.it.next());
			this.current++;
			return result;
		} catch (Exception e) {
			throw new RuntimeException(String.format("Failed to transform query result #%,d", this.current), e);
		}
	}

	@Override
	public void remove() {
		this.it.remove();
	}
}