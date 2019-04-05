package com.armedia.caliente.cli.ticketdecoder;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.dfc.DfUtils;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public abstract class ContentFinder implements Callable<Void> {

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
	public final Void call() throws DfException {
		final IDfSession session = this.pool.acquireSession();
		final IDfLocalTransaction tx = DfUtils.openTransaction(session);
		try {
			Stream<IDfId> ids = getIds(session);
			if (ids != null) {
				ids //
					.filter(Objects::nonNull) //
					.filter(IDfId::isObjectId) //
					.filter((id) -> this.scannedIds.add(id.getId())) //
					.forEach(this.consumer) //
				;
			}
			return null;
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