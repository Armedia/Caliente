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
package com.armedia.caliente.engine.local.exporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.tools.VersionNumberScheme;

public class NullVersionFinder extends LocalPathVersionFinder {

	public NullVersionFinder(VersionNumberScheme numberScheme) {
		super(numberScheme);
	}

	@Override
	protected Stream<Path> findSiblingCandidates(LocalRoot root, Path path) throws IOException {
		return Stream.empty();
	}

	@Override
	protected VersionInfo parseVersionInfo(LocalRoot root, Path p) {
		return null;
	}
}