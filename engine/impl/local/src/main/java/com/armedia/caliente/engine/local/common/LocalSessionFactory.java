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
package com.armedia.caliente.engine.local.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.common.SessionFactory;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class LocalSessionFactory extends SessionFactory<LocalRoot> {
	private final LocalRoot root;

	public LocalSessionFactory(CfgTools settings, CmfCrypt crypto) throws IOException {
		this(settings, LocalCommon.getLocalRoot(settings), crypto);
	}

	public LocalSessionFactory(CfgTools settings, LocalRoot root, CmfCrypt crypto) throws IOException {
		super(settings, crypto);
		this.root = root;
		final Path p = this.root.getPath();
		FileUtils.forceMkdir(p.toFile());
		if (!Files.isDirectory(p)) {
			throw new IllegalArgumentException(
				String.format("Root directory [%s] could not be found, nor could it be created", root));
		}
	}

	protected LocalRoot getRoot() {
		return this.root;
	}

	@Override
	public PooledObject<LocalRoot> makeObject() throws Exception {
		return new DefaultPooledObject<>(this.root);
	}

	@Override
	public void destroyObject(PooledObject<LocalRoot> p) throws Exception {
	}

	@Override
	public boolean validateObject(PooledObject<LocalRoot> p) {
		return true;
	}

	@Override
	public void activateObject(PooledObject<LocalRoot> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<LocalRoot> p) throws Exception {
	}

	@Override
	protected LocalSessionWrapper newWrapper(LocalRoot session) {
		return new LocalSessionWrapper(this, session);
	}
}