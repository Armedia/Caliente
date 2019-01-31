package com.armedia.caliente.engine.xml.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class XmlSessionFactory extends SessionFactory<XmlRoot> {
	private final XmlRoot root;

	public XmlSessionFactory(CfgTools settings, CmfCrypt crypto) throws IOException {
		super(settings, crypto);
		File root = XmlCommon.getRootDirectory(settings);
		if (root == null) {
			throw new IllegalArgumentException("Must provide a root directory to base the local engine off of");
		}
		root = root.getCanonicalFile();

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