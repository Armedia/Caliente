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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class XmlSessionFactory extends SessionFactory<XmlRoot> {
	private final XmlRoot root;

	public XmlSessionFactory(CfgTools settings, CmfCrypt crypto) throws IOException {
		super(settings, crypto);
		File root = XmlCommon.getRootDirectory(settings);
		if (root == null) {
			throw new IllegalArgumentException("Must provide a root directory to base the local engine off of");
		}
		root = Tools.canonicalize(root);

		FileUtils.forceMkdir(root);
		if (!root.isDirectory()) {
			throw new IllegalArgumentException(
				String.format("Root directory [%s] could not be found, nor could it be created", root));
		}
		this.root = new XmlRoot(root);
	}

	protected XmlRoot getRoot() {
		return this.root;
	}

	@Override
	public PooledObject<XmlRoot> makeObject() throws Exception {
		return new DefaultPooledObject<>(this.root);
	}

	@Override
	public void destroyObject(PooledObject<XmlRoot> p) throws Exception {
	}

	@Override
	public boolean validateObject(PooledObject<XmlRoot> p) {
		return true;
	}

	@Override
	public void activateObject(PooledObject<XmlRoot> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<XmlRoot> p) throws Exception {
	}

	@Override
	protected XmlSessionWrapper newWrapper(XmlRoot session) {
		return new XmlSessionWrapper(this, session);
	}
}