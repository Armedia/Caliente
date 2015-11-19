package com.armedia.cmf.engine.xml.exporter;

import java.io.File;

import com.armedia.cmf.engine.exporter.ExportDelegateFactory;
import com.armedia.cmf.engine.xml.common.XmlCommon;
import com.armedia.cmf.engine.xml.common.XmlFile;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.engine.xml.common.Setting;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlExportDelegateFactory extends
	ExportDelegateFactory<XmlRoot, XmlSessionWrapper, CmfValue, XmlExportContext, XmlExportEngine> {

	private final XmlRoot root;
	private final boolean copyContent;
	private final boolean includeAllVersions;

	protected XmlExportDelegateFactory(XmlExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		File root = XmlCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		this.root = new XmlRoot(root);
		this.copyContent = configuration.getBoolean(Setting.COPY_CONTENT);
		this.includeAllVersions = configuration.getBoolean(Setting.INCLUDE_ALL_VERSIONS);
	}

	public final XmlRoot getRoot() {
		return this.root;
	}

	public final boolean isCopyContent() {
		return this.copyContent;
	}

	public final boolean isIncludeAllVersions() {
		return this.includeAllVersions;
	}

	@Override
	protected XmlExportDelegate newExportDelegate(XmlRoot session, CmfType type, String searchKey) throws Exception {
		return new XmlExportDelegate(this, XmlFile.newFromSafePath(session, searchKey));
	}
}