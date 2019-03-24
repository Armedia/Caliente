package com.armedia.caliente.cli.ticketdecoder;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class ExtractorLogic implements PooledWorkersLogic<IDfSession, IDfId> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final DfcSessionPool pool;
	private final Predicate<Content> contentPredicate;
	private final Predicate<Rendition> renditionPredicate;
	private final Consumer<Content> contentConsumer;

	public ExtractorLogic(DfcSessionPool pool, Consumer<Content> contentConsumer, Predicate<Content> contentPredicate,
		Predicate<Rendition> renditionPredicate) {
		this.contentPredicate = contentPredicate;
		this.renditionPredicate = renditionPredicate;
		this.pool = pool;
		this.contentConsumer = contentConsumer;
	}

	@Override
	public IDfSession initialize() throws Exception {
		return this.pool.acquireSession();
	}

	@Override
	public void process(IDfSession session, IDfId id) throws Exception {
		if (id == null) { return; }
		final IDfLocalTransaction tx = DfUtils.openTransaction(session);
		try {
			Content c = getContent(session, id);
			if (c == null) { return; }
			if ((this.contentPredicate == null) || this.contentPredicate.test(c)) {
				this.contentConsumer.accept(c);
			}
		} finally {
			try {
				// No matter what...roll back!
				DfUtils.abortTransaction(session, tx);
			} catch (DfException e) {
				this.log.warn("Could not abort an open transaction", e);
			}
		}
	}

	private IDfSysObject getSysObject(IDfSession session, IDfId id) throws DfException {
		final IDfPersistentObject object;
		try {
			object = session.getObject(id);
		} catch (DfIdNotFoundException e) {
			return null;
		}

		if (object.isInstanceOf("dm_folder")) { return null; }
		if (!object.isInstanceOf("dm_sysobject")) { return null; }

		return IDfSysObject.class.cast(object);

	}

	private String getObjectPath(IDfSession session, IDfSysObject document) throws DfException {
		if (document.getHasFolder()) {
			IDfId parentId = document.getFolderId(0);
			IDfFolder parent = session.getFolderBySpecification(parentId.getId());
			String path = (parent != null ? parent.getFolderPath(0) : String.format("<parent-%s-not-found>", parentId));
			return String.format("%s/%s", path, document.getObjectName());
		} else {
			return String.format("(unfiled)/%s", document.getObjectName());
		}
	}

	public String getExtension(IDfSession session, IDfId format) throws DfException {
		if ((format == null) || format.isNull() || !format.isObjectId()) { return null; }
		IDfPersistentObject obj = session.getObject(format);
		if (!obj.isInstanceOf("dm_format")) { return null; }
		IDfFormat f = IDfFormat.class.cast(obj);
		return f.getDOSExtension();
	}

	public String getFileStoreLocation(IDfSession session, IDfContent content) throws DfException {
		try {
			IDfPersistentObject obj = session.getObject(content.getStorageId());
			String root = obj.getString("root");
			if (!StringUtils.isBlank(root)) {
				obj = session.getObjectByQualification(
					String.format("dm_location where object_name = %s", DfUtils.quoteString(root)));
				if ((obj != null) && obj.hasAttr("file_system_path")) { return obj.getString("file_system_path"); }
			}
			return String.format("(no-path-for-store-%s)", content.getStorageId());
		} catch (DfIdNotFoundException e) {
			return String.format("(store-%s-not-found)", content.getStorageId());
		}
	}

	private Collection<Rendition> getRenditions(IDfSession session, IDfSysObject document) throws DfException {
		// Calculate both the path and the ticket location
		final String dql = "" //
			+ "select dcs.r_object_id " //
			+ "  from dmr_content_r dcr, dmr_content_s dcs " //
			+ " where dcr.r_object_id = dcs.r_object_id " //
			+ "   and dcr.parent_id = %s " //
			+ " order by dcs.rendition, dcr.page ";

		int index = 0;
		final IDfId id = document.getObjectId();
		IDfCollection results = DfUtils.executeQuery(session, String.format(dql, DfUtils.quoteString(id.getId())),
			IDfQuery.DF_EXECREAD_QUERY);
		try {

			final String prefix = DfUtils.getDocbasePrefix(session);
			final Collection<Rendition> renditions = new LinkedList<>();
			while (results.next()) {
				final IDfId contentId = results.getId("r_object_id");
				final int idx = (index++);
				final IDfContent content;
				try {
					content = IDfContent.class.cast(session.getObject(contentId));
				} catch (DfException e) {
					this.log.error("Failed to retrieve the content stream object # {} (with id = [{}]) for {}", idx,
						contentId, id, e);
					continue;
				}
				String streamPath = DfUtils.decodeDataTicket(prefix, content.getDataTicket(), '/');
				String extension = getExtension(session, content.getFormatId());
				if (StringUtils.isBlank(extension)) {
					extension = StringUtils.EMPTY;
				} else {
					extension = String.format(".%s", extension);
				}

				final String pathPrefix = getFileStoreLocation(session, content);
				final Rendition rendition = new Rendition() //
					.setNumber(content.getRendition()) //
					.setPage(content.getInt("page")) //
					.setLength(content.getContentSize()) //
					.setHash(content.getContentHash()) //
					.setModifier(Tools.coalesce(content.getString("page_modifier"), "")) //
					.setFormat(content.getString("full_format")) //
					.setPath(String.format("%s/%s%s", pathPrefix.replace('\\', '/'), streamPath, extension));
				if ((this.renditionPredicate == null) || this.renditionPredicate.test(rendition)) {
					renditions.add(rendition);
				}
			}
			return renditions;
		} finally {
			DfUtils.closeQuietly(results);
		}
	}

	protected final Content getContent(IDfSession session, IDfId id) throws DfException {
		IDfSysObject document = getSysObject(session, id);
		if (document == null) { return null; }
		String path = getObjectPath(session, document);
		Content c = new Content() //
			.setPath(path) //
			.setId(id.getId()) //
		;
		c.getRenditions().addAll(getRenditions(session, document));
		return c;
	}

	@Override
	public void handleFailure(IDfSession session, IDfId id, Exception raised) {
		this.log.error("Failed to retrieve the content data for [{}]", id, raised);
	}

	@Override
	public void cleanup(IDfSession session) {
		this.pool.releaseSession(session);
	}
}