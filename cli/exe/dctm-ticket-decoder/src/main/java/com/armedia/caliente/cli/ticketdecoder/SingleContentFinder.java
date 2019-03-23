package com.armedia.caliente.cli.ticketdecoder;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public class SingleContentFinder extends ContentFinder {

	public SingleContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source) {
		super(pool, scannedIds, source);
	}

	@Override
	protected Stream<IDfId> getIds(IDfSession session) throws DfException {
		if (StringUtils.isBlank(this.source)) { return Stream.empty(); }
		if (!this.source.startsWith("%")) { return Stream.empty(); }
		IDfId id = new DfId(this.source.substring(1));
		if (id.isNull()) { return Stream.empty(); }
		if (!id.isObjectId()) { return Stream.empty(); }
		return Stream.of(id);
	}

}