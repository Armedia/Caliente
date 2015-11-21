package com.armedia.cmf.engine.xml.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.xml.common.Setting;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.engine.xml.importer.jaxb.AclsT;
import com.armedia.cmf.engine.xml.importer.jaxb.AggregatorBase;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentsT;
import com.armedia.cmf.engine.xml.importer.jaxb.FoldersT;
import com.armedia.cmf.engine.xml.importer.jaxb.GroupsT;
import com.armedia.cmf.engine.xml.importer.jaxb.TypesT;
import com.armedia.cmf.engine.xml.importer.jaxb.UsersT;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;

public class XmlImportDelegateFactory extends
	ImportDelegateFactory<XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportEngine> {

	private static final String SCHEMA = "import.xsd";

	private final boolean includeAllVersions;
	private final boolean failOnCollisions;

	private final Map<CmfType, AggregatorBase<?>> xml;

	public XmlImportDelegateFactory(XmlImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		this.includeAllVersions = configuration.getBoolean(Setting.INCLUDE_ALL_VERSIONS);
		this.failOnCollisions = configuration.getBoolean(Setting.FAIL_ON_COLLISIONS);
		Map<CmfType, AggregatorBase<?>> xml = new EnumMap<CmfType, AggregatorBase<?>>(CmfType.class);
		xml.put(CmfType.TYPE, new TypesT());
		xml.put(CmfType.USER, new UsersT());
		xml.put(CmfType.GROUP, new GroupsT());
		xml.put(CmfType.ACL, new AclsT());
		xml.put(CmfType.FOLDER, new FoldersT());
		xml.put(CmfType.DOCUMENT, new DocumentsT());
		this.xml = Tools.freezeCopy(xml);
	}

	public final boolean isIncludeAllVersions() {
		return this.includeAllVersions;
	}

	public final boolean isFailOnCollisions() {
		return this.failOnCollisions;
	}

	protected <T> T getXmlObject(CmfType t, Class<T> klazz) {
		return klazz.cast(this.xml.get(t));
	}

	@Override
	protected XmlImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case USER:
				return new XmlUserImportDelegate(this, storedObject);
			case GROUP:
				return new XmlGroupImportDelegate(this, storedObject);
			case ACL:
				return new XmlAclImportDelegate(this, storedObject);
			case TYPE:
				return new XmlTypeImportDelegate(this, storedObject);
			case FOLDER:
				return new XmlFolderImportDelegate(this, storedObject);
			case DOCUMENT:
				return new XmlDocumentImportDelegate(this, storedObject);
			default:
				return null;
		}
	}

	protected File calculateConsolidatedFile(CmfType t) {
		return new File("/dev/null");
	}

	@Override
	public void close() {
		for (CmfType t : CmfType.values()) {
			AggregatorBase<?> root = this.xml.get(t);
			if ((root == null) || (root.getCount() == 0)) {
				// If there is no aggregator, or it's empty, skip it
				continue;
			}

			// There is an aggregator, so write out its file
			final File f = calculateConsolidatedFile(t);
			final OutputStream out;
			try {
				out = new FileOutputStream(f);
			} catch (FileNotFoundException e) {
				// TODO: Log this error
				continue;
			}
			boolean ok = false;
			String xml = null;
			try {
				xml = XmlTools.marshal(root, XmlImportDelegateFactory.SCHEMA, true);
				try {
					IOUtils.write(xml, out);
				} catch (IOException e) {
					// TODO: Dump out the generated XML to the log
				}
				ok = true;
			} catch (JAXBException e) {
				// TODO: Log this error
			} finally {
				IOUtils.closeQuietly(out);
				if (!ok) {
					FileUtils.deleteQuietly(f);
				}
			}
		}

		super.close();
	}
}