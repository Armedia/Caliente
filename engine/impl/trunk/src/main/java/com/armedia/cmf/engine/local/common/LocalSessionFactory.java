package com.armedia.cmf.engine.local.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.h2.store.fs.FileUtils;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;

public class LocalSessionFactory extends SessionFactory<RootPath> {
	public static final String ROOT = "root";

	private final RootPath root;

	public LocalSessionFactory(CfgTools settings) throws IOException {
		super(settings);
		File root = LocalCommon.getRootDirectory(settings);
		if (root == null) { throw new IllegalArgumentException(
			"Must provide a root directory to base the local engine off of"); }
		root = root.getCanonicalFile();
		FileUtils.createDirectories(root.getPath());
		if (!root.isDirectory()) { throw new IllegalArgumentException(String.format(
			"Root directory [%s] could not be found, nor could it be created", root)); }
		this.root = new RootPath(root);
	}

	protected RootPath getRoot() {
		return this.root;
	}

	@Override
	public PooledObject<RootPath> makeObject() throws Exception {
		return new DefaultPooledObject<RootPath>(this.root);
	}

	@Override
	public void destroyObject(PooledObject<RootPath> p) throws Exception {
	}

	@Override
	public boolean validateObject(PooledObject<RootPath> p) {
		return true;
	}

	@Override
	public void activateObject(PooledObject<RootPath> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<RootPath> p) throws Exception {
	}

	@Override
	protected LocalSessionWrapper newWrapper(RootPath session) {
		return new LocalSessionWrapper(this, session);
	}
}