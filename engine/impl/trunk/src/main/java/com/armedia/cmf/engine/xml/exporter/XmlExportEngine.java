package com.armedia.cmf.engine.xml.exporter;

import java.util.Iterator;
import java.util.Set;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.xml.common.XmlCommon;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionFactory;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.engine.xml.common.XmlTranslator;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlExportEngine
	extends
	ExportEngine<XmlRoot, XmlSessionWrapper, CmfValue, XmlExportContext, XmlExportContextFactory, XmlExportDelegateFactory> {

	@Override
	protected Iterator<ExportTarget> findExportResults(XmlRoot session, CfgTools configuration,
		XmlExportDelegateFactory factory) throws Exception {
		return new XmlRecursiveIterator(session, true);
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new XmlTranslator();
	}

	@Override
	protected XmlSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new XmlSessionFactory(cfg);
	}

	@Override
	protected XmlExportContextFactory newContextFactory(XmlRoot root, CfgTools cfg) throws Exception {
		return new XmlExportContextFactory(this, cfg, root);
	}

	@Override
	protected XmlExportDelegateFactory newDelegateFactory(XmlRoot root, CfgTools cfg) throws Exception {
		return new XmlExportDelegateFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return XmlCommon.TARGETS;
	}

	public static ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(XmlCommon.TARGET_NAME);
	}
}