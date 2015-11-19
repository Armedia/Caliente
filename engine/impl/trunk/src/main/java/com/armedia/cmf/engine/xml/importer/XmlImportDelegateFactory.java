package com.armedia.cmf.engine.xml.importer;

import java.io.IOException;

import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.engine.xml.common.Setting;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlImportDelegateFactory extends
	ImportDelegateFactory<XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportEngine> {

	private final boolean includeAllVersions;
	private final boolean failOnCollisions;

	public XmlImportDelegateFactory(XmlImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		this.includeAllVersions = configuration.getBoolean(Setting.INCLUDE_ALL_VERSIONS);
		this.failOnCollisions = configuration.getBoolean(Setting.FAIL_ON_COLLISIONS);
	}

	public final boolean isIncludeAllVersions() {
		return this.includeAllVersions;
	}

	public final boolean isFailOnCollisions() {
		return this.failOnCollisions;
	}

	@Override
	protected XmlImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case DOCUMENT:
				return new XmlDocumentImportDelegate(this, storedObject);
			case FOLDER:
				return new XmlFolderImportDelegate(this, storedObject);
			default:
				return null;
		}
	}
}