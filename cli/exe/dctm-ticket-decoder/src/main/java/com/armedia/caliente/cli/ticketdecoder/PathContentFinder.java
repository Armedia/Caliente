package com.armedia.caliente.cli.ticketdecoder;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class PathContentFinder extends PredicateContentFinder {

	public PathContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source, Consumer<IDfId> consumer) {
		super(pool, scannedIds, source, consumer);
	}

	@Override
	protected Stream<IDfId> getIds(IDfSession session) throws DfException {
		// Source is a path to either a folder or a document
		IDfPersistentObject obj = session.getObjectByPath(this.source);
		if (obj == null) { return null; }

		final IDfId id = obj.getObjectId();
		if (!obj.isInstanceOf("dm_folder")) {
			// Not a folder, so no recursion!
			return Stream.of(id);
		}

		// If it's a folder, we morph into a query-based recursion.
		return super.getIds(session,
			String.format("dm_sysobject where folder(id(%s), DESCEND)", DfUtils.quoteString(id.getId())));
	}
}