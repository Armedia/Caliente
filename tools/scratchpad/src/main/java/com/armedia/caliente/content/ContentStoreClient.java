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
package com.armedia.caliente.content;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.function.LazySupplier;

public class ContentStoreClient {
	// TODO: Fix this for something meaningful and reproducible ... maybe based on IP addresses?
	private static final LazySupplier<String> ID = new LazySupplier<>(UUID.randomUUID()::toString);

	private final String applicationName;
	private final String id;

	private final AtomicLong fileId = new AtomicLong(0);

	public ContentStoreClient(String applicationName) {
		this(applicationName, null);
	}

	public ContentStoreClient(String applicationName, String id) {
		this.applicationName = applicationName;
		if (StringUtils.isEmpty(id)) {
			id = ContentStoreClient.ID.get();
		}
		this.id = id;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public String getId() {
		return this.id;
	}

	public long getNextFileId() {
		return this.fileId.getAndIncrement();
	}
}