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
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionFactory;
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

	private final Map<CmfType, AggregatorBase<?>> xml;
	private final File db;
	private final File content;

	public XmlImportDelegateFactory(XmlImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		String db = configuration.getString(XmlSessionFactory.DB);
		if (db != null) {
			this.db = new File(db).getCanonicalFile();
		} else {
			this.db = new File("cmsmf-xml").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.db);
		String content = configuration.getString(XmlSessionFactory.CONTENT);
		if (content != null) {
			this.content = new File(content).getCanonicalFile();
		} else {
			this.content = new File(db, "content").getCanonicalFile();
		}
		FileUtils.forceMkdir(this.content);

		Map<CmfType, AggregatorBase<?>> xml = new EnumMap<CmfType, AggregatorBase<?>>(CmfType.class);
		xml.put(CmfType.TYPE, new TypesT());
		xml.put(CmfType.USER, new UsersT());
		xml.put(CmfType.GROUP, new GroupsT());
		xml.put(CmfType.ACL, new AclsT());
		xml.put(CmfType.FOLDER, new FoldersT());
		xml.put(CmfType.DOCUMENT, new DocumentsT());
		this.xml = Tools.freezeCopy(xml);
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
		return new File(this.db, String.format("all-%s.xml", t.name().toLowerCase()));
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
					e.hashCode();
				}
				ok = true;
			} catch (JAXBException e) {
				// TODO: Log this error
				this.log.error(String.format("Failed to generate the XML for %s", t), e);
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