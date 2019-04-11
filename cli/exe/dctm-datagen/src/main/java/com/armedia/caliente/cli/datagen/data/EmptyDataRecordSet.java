package com.armedia.caliente.cli.datagen.data;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public final class EmptyDataRecordSet<D extends Object, R extends Object, S extends Object>
	extends DataRecordSet<D, R, S> {

	private static final Map<String, Integer> COLUMNS = Collections.singletonMap("COLUMN", 0);

	public EmptyDataRecordSet() throws Exception {
		super(false, 0, null);
	}

	@Override
	protected D initData() {
		return null;
	}

	@Override
	protected Map<String, Integer> mapColumns(D data) {
		return EmptyDataRecordSet.COLUMNS;
	}

	@Override
	protected Iterator<R> getIterator(D data) {
		return Collections.emptyIterator();
	}

	@Override
	protected DataRecord newRecord(R record) {
		throw new IllegalStateException("Can't produce a record");
	}

	@Override
	protected void closeData(D data) {
	}
}