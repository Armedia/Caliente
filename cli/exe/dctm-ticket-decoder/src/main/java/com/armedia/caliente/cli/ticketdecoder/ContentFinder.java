package com.armedia.caliente.cli.ticketdecoder;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public abstract class ContentFinder implements Callable<String> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Set<String> scannedIds;
	private final DfcSessionPool pool;
	protected final String source;
	private final Consumer<IDfId> consumer;

	/**
	 * @param pool
	 */
	public ContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source, Consumer<IDfId> consumer) {
		this.scannedIds = scannedIds;
		this.pool = pool;
		this.source = source;
		this.consumer = consumer;
	}

	@Override
	public final String call() throws Exception {
		final IDfSession session = this.pool.acquireSession();
		final IDfLocalTransaction tx = DfUtils.openTransaction(session);
		try {
			getIds(session) //
				.filter(Objects::nonNull) //
				.filter(IDfId::isObjectId) //
				.filter((id) -> this.scannedIds.add(id.getId())) //
				.forEach(this.consumer) //
			;
			return this.source;
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

}