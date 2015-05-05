package com.armedia.cmf.engine.local.common;

import java.io.File;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.h2.store.fs.FileUtils;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;

public class LocalSessionFactory extends SessionFactory<File> {
	public static final String ROOT = "root";

	private final File rootFile;

	public LocalSessionFactory(CfgTools settings) {
		super(settings);
		String root = settings.getString(LocalSessionFactory.ROOT);
		if (root == null) { throw new IllegalArgumentException(
			"Must provide a root directory to base the local engine off of"); }
		this.rootFile = new File(root);
		FileUtils.createDirectories(root);
		if (!this.rootFile.isDirectory()) { throw new IllegalArgumentException(String.format(
			"Root directory [%s] could not be found, nor could it be created", root)); }
	}

	@Override
	public PooledObject<File> makeObject() throws Exception {
		return new DefaultPooledObject<File>(this.rootFile);
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