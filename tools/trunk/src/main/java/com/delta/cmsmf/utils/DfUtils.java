package com.delta.cmsmf.utils;

import org.apache.log4j.Logger;

import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public class DfUtils {

	private static final Logger LOG = Logger.getLogger(DfUtils.class);

	public static void closeQuietly(IDfCollection c) {
		if (c == null) { return; }
		try {
			c.close();
		} catch (DfException e) {
			// quietly swallowed
			if (DfUtils.LOG.isTraceEnabled()) {
				DfUtils.LOG.trace("Swallowing exception on close", e);
			}
		}
	}

	public static IDfQuery newQuery() {
		return new DfClientX().getQuery();
	}

	public static IDfCollection executeQuery(IDfSession session, String dql) throws DfException {
		return DfUtils.executeQuery(session, dql, IDfQuery.DF_QUERY);
	}

	public static IDfCollection executeQuery(IDfSession session, String dql, int queryType) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to execute the DQL on"); }
		if (dql == null) { throw new IllegalArgumentException("Must provide a DQL statement to execute"); }
		IDfQuery query = DfUtils.newQuery();
		if (DfUtils.LOG.isTraceEnabled()) {
			DfUtils.LOG.trace(String.format("Executing DQL (type=%d): %s", queryType, dql));
		}
		query.setDQL(dql);
		return query.execute(session, queryType);
	}
}