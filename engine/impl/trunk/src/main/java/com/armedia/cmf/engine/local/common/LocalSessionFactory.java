package com.armedia.cmf.engine.local.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.h2.store.fs.FileUtils;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;

public class LocalSessionFactory extends SessionFactory<File> {
	public static final String ROOT = "root";

	private final File root;

	public LocalSessionFactory(CfgTools settings) throws IOException {
		super(settings);
		File root = LocalCommon.getRootDirectory(settings);
		if (root == null) { throw new IllegalArgumentException(
			"Must provide a root directory to base the local engine off of"); }
		root = root.getCanonicalFile();
		FileUtils.createDirectories(root.getCanonicalPath());
		if (!root.isDirectory()) { throw new IllegalArgumentException(String.format(
			"Root directory [%s] could not be found, nor could it be created", root)); }
		this.root = root;
	}

	protected File getRoot() {
		return this.root;
	}

	@Override
	public PooledObject<File> makeObject() throws Exception {
		return new DefaultPooledObject<File>(this.root);
	}

	@Override
	public void destroyObject(PooledObject<File> p) throws Exception {
	}

	@Override
	public boolean validateObject(PooledObject<File> p) {
		return true;
	}

	@Override
	public void activateObject(PooledObject<File> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<File> p) throws Exception {
	}

	@Override
	protected LocalSessionWrapper newWrapper(File session) {
		return new LocalSessionWrapper(this, session);
	}
}