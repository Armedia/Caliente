package com.armedia.caliente.engine.local.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class LocalSessionFactory extends SessionFactory<LocalRoot> {
	private final LocalRoot root;

	public LocalSessionFactory(CfgTools settings, CmfCrypt crypto) throws IOException {
		super(settings, crypto);
		File root = LocalCommon.getRootDirectory(settings);
		if (root == null) {
			throw new IllegalArgumentException("Must provide a root directory to base the local engine off of");
		}
		root = root.getCanonicalFile();

		FileUtils.forceMkdir(root);
		if (!root.isDirectory()) {
			throw new IllegalArgumentException(
				String.format("Root directory [%s] could not be found, nor could it be created", root));
		}
		this.root = new LocalRoot(root);
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