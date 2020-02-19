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
package com.armedia.caliente.engine.sql.common;

import java.io.File;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public final class SqlCommon {
	public static final String TARGET_NAME = "local";
	public static final Set<String> TARGETS = Collections.singleton(SqlCommon.TARGET_NAME);

	private SqlCommon() {
	}

	public static File getRootDirectory(CfgTools cfg) {
		String root = cfg.getString(SqlSetting.ROOT);
		if (root == null) { return null; }
		return Tools.canonicalize(new File(root));
	}

	public static String calculateId(String portablePath) {
		if ((portablePath == null) || Objects.equals("/", portablePath)) { return null; }
		return DigestUtils.sha256Hex(portablePath);
	}

	public static String getPortablePath(String path) {
		if (StringUtils.isEmpty(path)) { return null; }
		return FileNameTools.reconstitute(FileNameTools.tokenize(path), true, false, '/');
	}
}