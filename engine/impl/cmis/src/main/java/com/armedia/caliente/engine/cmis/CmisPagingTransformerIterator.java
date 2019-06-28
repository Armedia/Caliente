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
package com.armedia.caliente.engine.cmis;

import java.util.Iterator;

import org.apache.chemistry.opencmis.client.api.ItemIterable;

public final class CmisPagingTransformerIterator<S, T> implements Iterator<T> {

	private final CmisPagingIterator<S> it;
	private final CmisResultTransformer<S, T> transformer;

	public CmisPagingTransformerIterator(ItemIterable<S> results, CmisResultTransformer<S, T> transformer) {
		if (transformer == null) { throw new IllegalArgumentException("Must provide a transformer"); }
		this.transformer = transformer;
		this.it = new CmisPagingIterator<>(results);
	}

	@Override
	public boolean hasNext() {
		return this.it.hasNext();
	}

	@Override
	public T next() {
		try {
			return this.transformer.transform(this.it.next());
		} catch (Exception e) {
			throw new RuntimeException(String.format("Failed to transform query result #%d", this.it.getCount()), e);
		}
	}

	@Override
	public void remove() {
		this.it.remove();
	}
}