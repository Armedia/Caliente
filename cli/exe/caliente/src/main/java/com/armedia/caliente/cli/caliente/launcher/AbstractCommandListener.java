package com.armedia.caliente.cli.caliente.launcher;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

class AbstractCommandListener {

	protected static final Integer PROGRESS_INTERVAL = 5;

	protected final Logger console;
	protected final CalienteWarningTracker warningTracker;
	protected final AtomicLong progressReporter = new AtomicLong(System.currentTimeMillis());

	protected AbstractCommandListener(Logger console) {
		this.console = console;
		this.warningTracker = new CalienteWarningTracker(console, true);
	}

	public final CalienteWarningTracker getWarningTracker() {
		return this.warningTracker;
	}

}