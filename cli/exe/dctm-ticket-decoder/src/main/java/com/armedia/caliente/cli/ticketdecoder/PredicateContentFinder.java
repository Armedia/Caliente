package com.armedia.caliente.cli.ticketdecoder;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DctmQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class PredicateContentFinder extends ContentFinder {

	public PredicateContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source,
		Consumer<IDfId> consumer) {
		super(pool, scannedIds, source, consumer);
	}

	@Override
	protected Stream<IDfId> getIds(IDfSession session) throws DfException {
		return getIds(session, this.source);
	}

	protected Stream<IDfId> getIds(IDfSession session, String predicate) throws DfException {
		String dqlQuery = String.format("select r_object_id from %s", predicate);
		@SuppressWarnings("resource")
		DctmQuery query = new DctmQuery(session, dqlQuery, DctmQuery.Type.DF_EXECREAD_QUERY);
		return query.stream().map(this::extractObjectId);
	}

	protected IDfId extractObjectId(IDfTypedObject o) {
		try {
			return o.getId("r_object_id");
		} catch (DfException e) {
			throw new RuntimeException("Failed to read the r_object_id column from the current DQL collection", e);
		}
	}
}