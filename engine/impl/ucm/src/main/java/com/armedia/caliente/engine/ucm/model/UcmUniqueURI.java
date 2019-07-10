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
package com.armedia.caliente.engine.ucm.model;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import com.armedia.commons.utilities.Tools;

public final class UcmUniqueURI implements Comparable<UcmUniqueURI>, Serializable {
	private static final long serialVersionUID = 1L;

	public static final UcmUniqueURI NULL_GUID;

	static {
		try {
			NULL_GUID = new UcmUniqueURI(new URI("null", "null", null));
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to initialize the NULL_GUID GUID", e);
		}
	}

	private final URI uri;

	public UcmUniqueURI(URI uri) {
		this.uri = Objects.requireNonNull(uri, "Must provide a non-null URI");
		if (UcmModel.isFileURI(uri)) {
			Objects.requireNonNull(uri.getFragment(),
				"A file's unique URI must also contain a revision ID in the fragment");
		}
	}

	public URI getURI() {
		return this.uri;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.uri);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		UcmUniqueURI other = UcmUniqueURI.class.cast(obj);
		if (!Tools.equals(this.uri, other.uri)) { return false; }
		return true;
	}

	@Override
	public int compareTo(UcmUniqueURI o) {
		if (o == null) { return 1; }
		return Tools.compare(this.uri, o.uri);
	}

	@Override
	public String toString() {
		return this.uri.toString();
	}
}