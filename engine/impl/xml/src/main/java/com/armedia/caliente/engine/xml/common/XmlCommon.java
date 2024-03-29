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
package com.armedia.caliente.engine.xml.common;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import com.armedia.commons.utilities.CfgTools;

public final class XmlCommon {
	public static final String DEFAULT_ATTRIBUTE_PREFIX = "caliente";
	public static final String TARGET_NAME = "xml";
	public static final String METADATA_ROOT = "xml-metadata";
	public static final Set<String> TARGETS = Collections.singleton(XmlCommon.TARGET_NAME);

	private XmlCommon() {

	}

	public static Path getRootDirectory(CfgTools cfg) throws IOException {
		String root = cfg.getString(XmlSetting.ROOT);
		if (root == null) { return null; }
		return Paths.get(root).normalize().toAbsolutePath();
	}

	public static Path getMetadataRoot(Path content) {
		return content.resolveSibling(XmlCommon.METADATA_ROOT);
	}
}