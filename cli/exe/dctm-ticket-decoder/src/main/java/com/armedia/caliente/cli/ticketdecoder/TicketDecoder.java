package com.armedia.caliente.cli.ticketdecoder;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public abstract class TicketDecoder implements Callable<Collection<Triple<IDfId, String, String>>> {

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
	public final Collection<Triple<IDfId, String, String>> call() throws Exception {
		final IDfSession session = this.pool.acquireSession();
		final IDfLocalTransaction tx = DfUtils.openTransaction(session);
		try {
			return getIds(session) //
				.filter(Objects::nonNull) //
				.filter(IDfId::isObjectId) //
				.filter((id) -> this.scannedIds.add(id.getId())) //
				.map((id) -> getMapping(session, id)) //
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

	protected final Triple<IDfId, String, String> getMapping(IDfSession session, IDfId id) {
		// Calculate both the path and the ticket location
		String path = null;

		String contentPath = null;

		return Triple.of(id, path, contentPath);
	}
}