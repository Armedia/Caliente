package com.armedia.caliente.cli.ticketdecoder;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Page;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DctmQuery;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class ExtractorLogic implements PooledWorkersLogic<IDfSession, IDfId, Exception> {

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
	public IDfSession initialize() throws DfException {
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

	private void findObjectPaths(IDfSession session, IDfSysObject document, Consumer<String> target)
		throws DfException {
		String objectName = document.getObjectName();
		if (StringUtils.isBlank(objectName)) {
			objectName = String.format("(blank-object-name-%s)", document.getObjectId().getId());
		}
		if (document.getHasFolder()) {
			final int pathCount = document.getFolderIdCount();
			for (int i = 0; i < pathCount; i++) {
				IDfId parentId = document.getFolderId(i);
				IDfFolder parent = session.getFolderBySpecification(parentId.getId());
				if (parent != null) {
					final int parentPathCount = parent.getFolderPathCount();
					for (int j = 0; j < parentPathCount; j++) {
						target.accept(String.format("%s/%s", parent.getFolderPath(j), document.getObjectName()));
					}
				} else {
					target.accept(String.format("<parent-%s-not-found>/%s", parentId, objectName));
				}
			}
		} else {
			target.accept(String.format("(unfiled)/%s", objectName));
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

	private Long findRenditions(IDfSession session, IDfSysObject document, Consumer<Rendition> target)
		throws DfException {
		// Calculate both the path and the ticket location
		final String dql = "" //
			+ "select dcs.r_object_id " //
			+ "  from dmr_content_r dcr, dmr_content_s dcs " //
			+ " where dcr.r_object_id = dcs.r_object_id " //
			+ "   and dcr.parent_id = %s " //
			+ " order by dcs.rendition, dcs.full_format, dcr.page_modifier, dcr.page ";

		int index = 0;
		final IDfId id = document.getObjectId();
		Long maxRendition = null;
		try (DctmQuery query = new DctmQuery(session, String.format(dql, DfUtils.quoteString(id.getId())),
			DctmQuery.Type.DF_EXECREAD_QUERY)) {

			final String prefix = DfUtils.getDocbasePrefix(session);
			Rendition rendition = null;
			while (query.hasNext()) {
				final IDfId contentId = query.next().getId("r_object_id");
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

				if ((rendition == null) || !rendition.matches(content)) {
					if ((rendition != null)
						&& ((this.renditionPredicate == null) || this.renditionPredicate.test(rendition))) {
						rendition.setPageCount(rendition.getPages().size());
						target.accept(rendition);
					}
					rendition = new Rendition() //
						.setType(content.getRendition()) //
						.setFormat(content.getString("full_format")) //
						.setModifier(Tools.coalesce(content.getString("page_modifier"), "")) //
					;
				}

				rendition.getPages().add(new Page() //
					.setNumber(content.getInt("page")) //
					.setLength(content.getContentSize()) //
					.setHash(content.getContentHash()) //
					.setPath(String.format("%s/%s%s", pathPrefix.replace('\\', '/'), streamPath, extension)));
			}
			if ((rendition != null) && ((this.renditionPredicate == null) || this.renditionPredicate.test(rendition))) {
				rendition.setPageCount(rendition.getPages().size());
				target.accept(rendition);
			}
			return maxRendition;
		}
	}

	protected final Content getContent(IDfSession session, IDfId id) throws DfException {
		IDfSysObject document = getSysObject(session, id);
		if (document == null) { return null; }
		Content c = new Content() //
			.setId(id.getId()) //
		;
		findObjectPaths(session, document, c.getPaths()::add);
		findRenditions(session, document, c.getRenditions()::add);
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