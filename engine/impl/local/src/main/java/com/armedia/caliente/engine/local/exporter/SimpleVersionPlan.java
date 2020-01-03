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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.tools.VersionNumberScheme;

/**
 * <p>
 * {@code create cached global temporary table if not exists table_name ( columns... ) transactional;}
 * </p>
 * <p>
 * No tag: {@code <parent/prefix[.ext]>}
 * </p>
 * <p>
 * With tag: {@code <parent/prefix[.ext]>.v(versionTag)}
 * </p>
 *
 * @author diego
 *
 */
public class SimpleVersionPlan extends LocalVersionPlan {

	private final Pattern pattern;

	public SimpleVersionPlan(VersionNumberScheme numberScheme) {
		super(numberScheme, null);
		this.pattern = Pattern.compile("^(.*?)(?:.v" + numberScheme.toPattern().pattern() + ")?$");
	}

	@Override
	protected VersionInfo parseVersionInfo(LocalRoot root, Path path) {
		Matcher m = this.pattern.matcher(path.toString());
		final Path rawRadix = (m.matches() ? Paths.get(m.group(1)) : path);
		final Path radix = LocalCommon.uncheck(() -> root.relativize(rawRadix));
		return new VersionInfo(path, radix, m.group(2));
	}

	protected boolean siblingCheck(LocalFile baseFile, Path candidate) throws IOException {
		if ((baseFile == null) || (candidate == null)) { return false; }

		// If either is not a regular file, they're not siblings
		Path basePath = baseFile.getAbsolute().toPath();
		if (Files.isDirectory(basePath) || Files.isDirectory(candidate)) { return false; }

		// If they're the same file, they're siblings
		if (Files.isSameFile(basePath, candidate)) { return true; }

		// If they share everything except the .vXXXXXX suffix, they're siblings

		// Same parent?
		final Path parentA = basePath.getParent();
		final Path parentB = candidate.getParent();
		if (!Files.isSameFile(parentA, parentB)) { return false; }

		// Same radix?
		final String nameA = basePath.getFileName().toString();
		final String nameB = candidate.getFileName().toString();

		final Matcher mA = this.pattern.matcher(nameA);
		final Matcher mB = this.pattern.matcher(nameB);

		return Objects.equals(getMatch(mA, 1, nameA), getMatch(mB, 1, nameB));
	}

	private String getMatch(Matcher m, int group, String s) {
		return (m.matches() ? m.group(group) : s);
	}
}