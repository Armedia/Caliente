package com.armedia.caliente.cli.ticketdecoder;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public class SingleContentFinder extends ContentFinder {

	public SingleContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source, Consumer<IDfId> consumer) {
		super(pool, scannedIds, source, consumer);
	}

	@Override
	protected Stream<IDfId> getIds(IDfSession session) throws DfException {
		if (StringUtils.isBlank(this.source)) { return null; }
		if (!this.source.startsWith("%")) { return null; }
		IDfId id = new DfId(this.source.substring(1));
		if (id.isNull()) { return null; }
		if (!id.isObjectId()) { return null; }
		return Stream.of(id);
	}

}