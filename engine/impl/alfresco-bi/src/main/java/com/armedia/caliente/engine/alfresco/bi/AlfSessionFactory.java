package com.armedia.caliente.engine.alfresco.bi;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.tools.PathTools;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class AlfSessionFactory extends SessionFactory<AlfRoot> {

	private final AlfRoot root;

	public AlfSessionFactory(CfgTools settings, CmfCrypt crypto) throws IOException {
		super(settings, crypto);
		File root = PathTools.getRootDirectory(settings);
		if (root == null) {
			throw new IllegalArgumentException("Must provide a root directory to base the local engine off of");
		}
		root = root.getCanonicalFile();

		FileUtils.forceMkdir(root);
		if (!root.isDirectory()) {
			throw new IllegalArgumentException(
				String.format("Root directory [%s] could not be found, nor could it be created", root));
		}
		this.root = new AlfRoot(root);
	}

	protected AlfRoot getRoot() {
		return this.root;
	}

	@Override
	public PooledObject<AlfRoot> makeObject() throws Exception {
		return new DefaultPooledObject<>(this.root);
	}

	@Override
	public void destroyObject(PooledObject<AlfRoot> p) throws Exception {
	}

	@Override
	public boolean validateObject(PooledObject<AlfRoot> p) {
		return true;
	}

	@Override
	public void activateObject(PooledObject<AlfRoot> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<AlfRoot> p) throws Exception {
	}

	@Override
	protected AlfSessionWrapper newWrapper(AlfRoot session) {
		return new AlfSessionWrapper(this, session);
	}
}