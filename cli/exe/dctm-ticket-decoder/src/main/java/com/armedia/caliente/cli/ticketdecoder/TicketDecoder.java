package com.armedia.caliente.cli.ticketdecoder;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public abstract class TicketDecoder implements Callable<Collection<Content>> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Set<String> scannedIds;
	private final DfcSessionPool pool;
	protected final String source;

	/**
	 * @param pool
	 */
	public TicketDecoder(DfcSessionPool pool, Set<String> scannedIds, String source) {
		this.scannedIds = scannedIds;
		this.pool = pool;
		this.source = source;
	}

	@Override
	public final Collection<Content> call() throws Exception {
		final IDfSession session = this.pool.acquireSession();
		final IDfLocalTransaction tx = DfUtils.openTransaction(session);
		try {
			return getIds(session) //
				.filter(Objects::nonNull) //
				.filter(IDfId::isObjectId) //
				.filter((id) -> this.scannedIds.add(id.getId())) //
				.map((id) -> getContent(session, id)) //
				.filter(Objects::nonNull) //
				.collect(Collectors.toCollection(LinkedList::new)) //
			;
		} finally {
			try {
				// No matter what...roll back!
				DfUtils.abortTransaction(session, tx);
			} catch (DfException e) {
				this.log.warn("Could not abort an open transaction", e);
			} finally {
				this.pool.releaseSession(session);
			}
		}
	}

	protected abstract Stream<IDfId> getIds(IDfSession session) throws DfException;

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
		return null;
	}

	public String getExtension(IDfSession session, IDfId format) throws DfException {
		if ((format == null) || format.isNull() || !format.isObjectId()) { return null; }
		IDfPersistentObject obj = session.getObject(format);
		if (!obj.isInstanceOf("dm_format")) { return null; }
		IDfFormat f = IDfFormat.class.cast(obj);
		return f.getDOSExtension();
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
				String relativePortion = DfUtils.decodeDataTicket(prefix, content.getDataTicket());
				String extension = getExtension(session, content.getFormatId());
				if (StringUtils.isBlank(extension)) {
					extension = StringUtils.EMPTY;
				} else {
					extension = String.format(".%s", extension);
				}

				renditions.add(new Rendition() //
					.setNumber(content.getRendition()) //
					.setPage(content.getInt("page")) //
					.setModifier(Tools.coalesce(content.getString("page_modifier"), "")) //
					.setFormat(content.getString("full_format")) //
					.setPath(String.format("%s%s", relativePortion, extension)));
			}
			return renditions;
		} finally {
			DfUtils.closeQuietly(results);
		}
	}

	protected final Content getContent(IDfSession session, IDfId id) {

		try {
			IDfSysObject document = getSysObject(session, id);
			String path = getObjectPath(session, document);
			Content c = new Content() //
				.setPath(path) //
				.setId(id.getId()) //
			;
			c.getRenditions().addAll(getRenditions(session, document));
			return c;
		} catch (DfException e) {
			this.log.error("Failed to retrieve the content data for [{}]", id, e);
			return null;
		}
	}
}