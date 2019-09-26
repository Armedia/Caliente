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
package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public abstract class FileContentPersistor extends ContentPersistor {
	private static final String BASE_NAME = "File";

	private final String name;

	protected Writer out = null;

	public FileContentPersistor(File target) {
		this(target, FileContentPersistor.BASE_NAME);
	}

	public FileContentPersistor(File target, String baseName) {
		super(Objects.requireNonNull(target));
		if (StringUtils.isBlank(baseName)) {
			baseName = FileContentPersistor.BASE_NAME;
		}
		if (this.target != null) {
			this.name = String.format("%s [%s]", baseName, target);
		} else {
			this.name = baseName;
		}
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	protected void startup() throws Exception {
		this.out = new FileWriter(this.target);
	}

	@Override
	protected void cleanup() throws Exception {
		if (this.out != null) {
			try {
				this.out.close();
			} finally {
				this.out = null;
			}
		}
	}
}