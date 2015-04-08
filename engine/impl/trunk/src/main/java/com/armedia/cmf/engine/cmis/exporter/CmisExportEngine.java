package com.armedia.cmf.engine.cmis.exporter;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.cmis.CmisCommon;
import com.armedia.cmf.engine.cmis.CmisSessionFactory;
import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportEngine extends
ExportEngine<Session, CmisSessionWrapper, CmisObject, Property<?>, CmisExportContext> {

	@Override
	protected String getObjectId(CmisObject sourceObject) {
		return sourceObject.getId();
	}

	@Override
	protected String calculateLabel(CmisObject sourceObject) throws Exception {
		return null;
	}

	@Override
	protected Iterator<ExportTarget> findExportResults(Session session, Map<String, ?> settings) throws Exception {
		return null;
	}

	@Override
	protected CmisObject getObject(Session session, StoredObjectType type, String searchKey) throws Exception {
		return session.getObject(searchKey);
	}

	@Override
	protected Collection<CmisObject> identifyRequirements(Session session, StoredObject<Property<?>> marshalled,
		CmisObject object, CmisExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected Collection<CmisObject> identifyDependents(Session session, StoredObject<Property<?>> marshalled,
		CmisObject object, CmisExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected ExportTarget getExportTarget(CmisObject object) throws ExportException {
		return new ExportTarget(decodeType(object.getType()), object.getId(), object.getId());
	}

	protected StoredObjectType decodeType(ObjectType type) throws ExportException {
		if (!type.isBaseType()) { return decodeType(type.getParentType()); }
		if (Tools.equals("cmis:folder", type.getId())) { return StoredObjectType.FOLDER; }
		if (Tools.equals("cmis:document", type.getId())) { return StoredObjectType.DOCUMENT; }
		return null;
	}

	@Override
	protected StoredObject<Property<?>> marshal(CmisExportContext ctx, Session session, CmisObject object)
		throws ExportException {
		return null;
	}

	@Override
	protected List<ContentInfo> storeContent(Session session, StoredObject<Property<?>> marshaled,
		ExportTarget referrent, CmisObject object, ContentStore streamStore) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to store the contents"); }
		if (marshaled == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		if (streamStore == null) { throw new IllegalArgumentException(
			"Must provide a stream store in which to store the content"); }
		List<ContentInfo> info = null;
		if (object instanceof Document) {
			info = new ArrayList<ContentInfo>();
			Document d = Document.class.cast(object);
			for (Rendition r : d.getRenditions()) {
				Handle h = streamStore.getHandle(marshaled, URLEncoder.encode(r.getKind(), "UTF-8"));
				ContentStream c = r.getContentStream();
				OutputStream out = null;
				InputStream in = null;
				try {
					out = h.openOutput();
					in = c.getStream();
					IOUtils.copy(in, out);
				} finally {
					IOUtils.closeQuietly(out);
					IOUtils.closeQuietly(in);
				}
			}
		}
		return info;
	}

	@Override
	protected Property<?> getValue(StoredDataType type, Object value) {
		return null;
	}

	@Override
	protected ObjectStorageTranslator<CmisObject, Property<?>> getTranslator() {
		return null;
	}

	@Override
	protected CmisSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new CmisSessionFactory(cfg);
	}

	@Override
	protected CmisExportContextFactory newContextFactory(CfgTools cfg) throws Exception {
		return new CmisExportContextFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return CmisCommon.TARGETS;
	}

	public static ExportEngine<?, ?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(CmisCommon.TARGET_NAME);
	}
}