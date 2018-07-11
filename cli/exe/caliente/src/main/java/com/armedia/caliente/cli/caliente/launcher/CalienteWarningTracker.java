package com.armedia.caliente.cli.caliente.launcher;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectRef;

public class CalienteWarningTracker implements WarningTracker {

	public static enum WarningType {
		//
		WARNING,
		//
		;
	}

	public static class Warning {
		private final long nanos;
		private final String threadName;
		private final CmfObjectRef ref;
		private final String message;

		private Warning(CmfObjectRef ref, String fmt, Object... args) {
			this.threadName = Thread.currentThread().getName();
			this.nanos = System.nanoTime();
			this.ref = new CmfObjectRef(ref);
			this.message = String.format(fmt, args);
		}

		public long getNanos() {
			return this.nanos;
		}

		public String getThreadName() {
			return this.threadName;
		}

		public CmfObjectRef getRef() {
			return this.ref;
		}

		public String getMessage() {
			return this.message;
		}

		public String generateReport() {
			return String.format("TRACKED WARNING ON %s: %s", this.ref.getShortLabel(), this.message);
		}

		public String toString(boolean withTime) {
			long millis = TimeUnit.MILLISECONDS.convert(this.nanos, TimeUnit.NANOSECONDS);
			String timeHeader = (withTime
				? String.format("%s ", DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(millis))
				: StringUtils.EMPTY);
			return String.format("%s[%s] %s", timeHeader, this.threadName, generateReport());
		}

		@Override
		public String toString() {
			return toString(true);
		}
	}

	private final Logger output;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final AtomicBoolean warningsAdded = new AtomicBoolean(false);
	private final CmfObjectCounter<WarningType> objectCounter = new CmfObjectCounter<>(WarningType.class);
	private final List<Warning> warnings;

	public CalienteWarningTracker() {
		this(null, false);
	}

	public CalienteWarningTracker(Logger output) {
		this(output, false);
	}

	public CalienteWarningTracker(boolean persistent) {
		this(null, persistent);
	}

	public CalienteWarningTracker(Logger output, boolean persistent) {
		if (output == null) {
			output = LoggerFactory.getLogger("warnings");
		}
		if (persistent) {
			this.warnings = new LinkedList<>();
		} else {
			this.warnings = null;
		}
		this.output = output;
	}

	private boolean persistWarning(Warning w) {
		if (this.warnings == null) { return false; }
		Lock l = this.lock.writeLock();
		l.lock();
		try {
			this.warnings.add(w);
			return true;
		} finally {
			l.unlock();
		}
	}

	@Override
	public void trackWarning(CmfObjectRef ref, String format, Object... args) {
		final Warning w = new Warning(ref, format, args);
		if (!persistWarning(w)) {
			this.output.warn(w.generateReport());
		}
		this.objectCounter.increment(ref.getType(), WarningType.WARNING);
		this.warningsAdded.compareAndSet(false, true);
	}

	public boolean hasWarnings() {
		return this.warningsAdded.get();
	}

	public String generateReport() {
		if (!hasWarnings()) { return null; }

		Lock l = this.lock.readLock();
		l.lock();
		try {
			Map<WarningType, Long> m = this.objectCounter.getCummulative();
			final Long zero = Long.valueOf(0);
			StringBuilder report = new StringBuilder();
			report.append(String.format("Tracked Warnings Summary:%n%n")).append(StringUtils.repeat("=", 30));
			for (WarningType t : WarningType.values()) {
				Long i = m.get(t);
				if (i == null) {
					i = zero;
				}
				report.append(String.format("%n%-16s : %8d", t.name(), i.intValue()));
			}

			if (this.warnings != null) {
				report.append("%n%nWarning Detail:%n%n");
				final String nl = String.format("%n");
				for (Warning w : this.warnings) {
					report.append("\t").append(w.toString(true)).append(nl);
				}
			}
			return report.toString();
		} finally {
			l.unlock();
		}
	}

	public void generateReport(Logger output) {
		if (!hasWarnings()) { return; }

		Lock l = this.lock.readLock();
		l.lock();
		try {
			Map<WarningType, Long> m = this.objectCounter.getCummulative();
			final Long zero = Long.valueOf(0);
			output.warn("Tracked Warnings Summary:");
			output.warn("");
			for (WarningType t : WarningType.values()) {
				Long i = m.get(t);
				if (i == null) {
					i = zero;
				}
				output.warn(String.format("%-16s : %8d", t.name(), i.intValue()));
			}

			if (this.warnings != null) {
				output.warn("");
				output.warn("Warning Detail:");
				output.warn("");
				for (Warning w : this.warnings) {
					output.warn(w.toString(true));
				}
			}
		} finally {
			l.unlock();
		}
	}
}