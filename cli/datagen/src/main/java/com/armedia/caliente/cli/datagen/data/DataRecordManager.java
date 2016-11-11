package com.armedia.caliente.cli.datagen.data;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.LockDispenser;
import com.armedia.commons.utilities.Tools;

public abstract class DataRecordManager<RS extends DataRecordSet<?, ?, ?>, L extends Object> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static final Set<String> REQUIRED_STREAM_COLUMNS;
	static {
		Set<String> s = new TreeSet<String>();
		s.add("format");
		s.add("location");
		REQUIRED_STREAM_COLUMNS = Tools.freezeSet(new LinkedHashSet<String>(s));
	}

	private boolean streamRecordsInitialized = false;
	private RS streamRecords = null;

	private final LockDispenser<String, Object> typeLocks = LockDispenser.getBasic();
	private final ConcurrentMap<String, RS> typeRecords = new ConcurrentHashMap<String, RS>();

	protected abstract RS buildRecordSet(L location, int loopCount) throws Exception;

	protected abstract String describeLocation(L location);

	private RS loadDataRecordSet(L location, int loopCount) {
		if (location == null) { return null; }
		try {
			return buildRecordSet(location, loopCount);
		} catch (Exception e) {
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Failed to load the data records from [%s]", describeLocation(location)),
					e);
			}
			return null;
		}
	}

	protected abstract L findStreamRecords() throws IOException;

	public final synchronized RS getStreamRecords(int loopCount) {
		if ((this.streamRecords == null) && !this.streamRecordsInitialized) {
			try {
				RS c = loadDataRecordSet(findStreamRecords(), loopCount);
				if (c == null) { return null; }
				Set<String> missing = new LinkedHashSet<String>(DataRecordManager.REQUIRED_STREAM_COLUMNS);
				missing.removeAll(c.getColumnNames());
				if (!missing.isEmpty()) { throw new Exception(
					String.format("The CSV stream records lack the following required columns: %s", missing)); }
				this.streamRecords = c;
			} catch (Exception e) {
				if (DataRecordManager.this.log.isDebugEnabled()) {
					DataRecordManager.this.log.debug("Failed to load the data stream CSV records", e);
				}
			} finally {
				this.streamRecordsInitialized = true;
			}
		}
		return this.streamRecords;
	}

	protected abstract L findTypeRecords(String type) throws IOException;

	public final RS getTypeRecords(final String type, final int loopCount) {
		final Object lock = this.typeLocks.getLock(type);
		if (!this.typeRecords.containsKey(type)) {
			synchronized (lock) {
				return ConcurrentUtils.createIfAbsentUnchecked(this.typeRecords, type, new ConcurrentInitializer<RS>() {
					@Override
					public RS get() throws ConcurrentException {
						try {
							return loadDataRecordSet(findTypeRecords(type), loopCount);
						} catch (Exception e) {
							if (DataRecordManager.this.log.isDebugEnabled()) {
								DataRecordManager.this.log
									.debug(String.format("Failed to locate the records for type [%s]", type), e);
							}
						}
						return null;
					}
				});
			}
		}
		return this.typeRecords.get(type);
	}
}