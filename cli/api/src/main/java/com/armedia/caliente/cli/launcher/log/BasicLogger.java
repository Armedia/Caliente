package com.armedia.caliente.cli.launcher.log;

import java.io.PrintStream;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import com.armedia.commons.utilities.Tools;

public class BasicLogger extends MarkerIgnoringBase implements Logger {
	private static final long serialVersionUID = 1L;

	public static enum Level {
		//
		NONE, ERROR, WARN, INFO, DEBUG, TRACE,
		//
		;
	}

	private static final Level DEFAULT_LEVEL = Level.INFO;

	private final Level level;

	public BasicLogger(String name) {
		this(name, null);
	}

	public BasicLogger(String name, Level level) {
		this.name = name;
		if (level == null) {
			String levelStr = System.getProperty("bootlog.level");
			try {
				level = Level.valueOf(levelStr.trim().toUpperCase());
			} catch (NullPointerException | IllegalArgumentException e) {
				// Bad level value - revert to the default
				level = BasicLogger.DEFAULT_LEVEL;
			}
		}
		this.level = level;
	}

	private String formatPrefix(Date date, Thread thread, Level level) {
		// Format the time, the thread, etc...
		// Mimic: %d{yyyy/MM/dd HH:mm:ss,SSS z} %-5p [%-16t] %c - %m%n
		return String.format("%s %-5s [%-16s] %s - ", DateFormatUtils.format(date, "yyyy/MM/dd HH:mm:ss,SSS z"),
			level.name().toUpperCase(), thread.getName(), this.name);
	}

	private void render(PrintStream str, Level level, FormattingTuple ft) {
		final Date now = new Date(System.currentTimeMillis());
		final Throwable thrown = ft.getThrowable();
		final String prefix = formatPrefix(now, Thread.currentThread(), level);
		if (thrown != null) {
			str.printf("%s%s%n%s%n", prefix, ft.getMessage(), Tools.dumpStackTrace(thrown));
		} else {
			str.printf("%s%s%n", prefix, ft.getMessage());
		}
	}

	protected Level getLevel() {
		return this.level;
	}

	protected boolean isEnabled(Level level) {
		return (this.level.ordinal() >= level.ordinal());
	}

	@Override
	public boolean isTraceEnabled() {
		return isEnabled(Level.TRACE);
	}

	@Override
	public void trace(String msg) {
		if (!isTraceEnabled()) { return; }
		render(System.out, Level.TRACE, MessageFormatter.format(msg, null));
	}

	@Override
	public void trace(String format, Object arg) {
		if (!isTraceEnabled()) { return; }
		render(System.out, Level.TRACE, MessageFormatter.format(format, arg));
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		if (!isTraceEnabled()) { return; }
		render(System.out, Level.TRACE, MessageFormatter.format(format, arg1, arg2));
	}

	@Override
	public void trace(String format, Object... arguments) {
		if (!isTraceEnabled()) { return; }
		render(System.out, Level.TRACE, MessageFormatter.arrayFormat(format, arguments));
	}

	@Override
	public void trace(String msg, Throwable t) {
		if (!isTraceEnabled()) { return; }
		render(System.out, Level.TRACE, MessageFormatter.format(msg, null, t));
	}

	@Override
	public boolean isDebugEnabled() {
		return isEnabled(Level.DEBUG);
	}

	@Override
	public void debug(String msg) {
		if (!isDebugEnabled()) { return; }
		render(System.out, Level.DEBUG, MessageFormatter.format(msg, null));
	}

	@Override
	public void debug(String format, Object arg) {
		if (!isDebugEnabled()) { return; }
		render(System.out, Level.DEBUG, MessageFormatter.format(format, arg));
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		if (!isDebugEnabled()) { return; }
		render(System.out, Level.DEBUG, MessageFormatter.format(format, arg1, arg2));
	}

	@Override
	public void debug(String format, Object... arguments) {
		if (!isDebugEnabled()) { return; }
		render(System.out, Level.DEBUG, MessageFormatter.arrayFormat(format, arguments));
	}

	@Override
	public void debug(String msg, Throwable t) {
		if (!isDebugEnabled()) { return; }
		render(System.out, Level.DEBUG, MessageFormatter.format(msg, null, t));
	}

	@Override
	public boolean isInfoEnabled() {
		return isEnabled(Level.INFO);
	}

	@Override
	public void info(String msg) {
		if (!isInfoEnabled()) { return; }
		render(System.out, Level.INFO, MessageFormatter.format(msg, null));
	}

	@Override
	public void info(String format, Object arg) {
		if (!isInfoEnabled()) { return; }
		render(System.out, Level.INFO, MessageFormatter.format(format, arg));
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		if (!isInfoEnabled()) { return; }
		render(System.out, Level.INFO, MessageFormatter.format(format, arg1, arg2));
	}

	@Override
	public void info(String format, Object... arguments) {
		if (!isInfoEnabled()) { return; }
		render(System.out, Level.INFO, MessageFormatter.arrayFormat(format, arguments));
	}

	@Override
	public void info(String msg, Throwable t) {
		if (!isInfoEnabled()) { return; }
		render(System.out, Level.INFO, MessageFormatter.format(msg, null, t));
	}

	@Override
	public boolean isWarnEnabled() {
		return isEnabled(Level.WARN);
	}

	@Override
	public void warn(String msg) {
		if (!isWarnEnabled()) { return; }
		render(System.err, Level.WARN, MessageFormatter.format(msg, null));
	}

	@Override
	public void warn(String format, Object arg) {
		if (!isWarnEnabled()) { return; }
		render(System.err, Level.WARN, MessageFormatter.format(format, arg));
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		if (!isWarnEnabled()) { return; }
		render(System.err, Level.WARN, MessageFormatter.format(format, arg1, arg2));
	}

	@Override
	public void warn(String format, Object... arguments) {
		if (!isWarnEnabled()) { return; }
		render(System.err, Level.WARN, MessageFormatter.arrayFormat(format, arguments));
	}

	@Override
	public void warn(String msg, Throwable t) {
		if (!isWarnEnabled()) { return; }
		render(System.err, Level.WARN, MessageFormatter.format(msg, null, t));
	}

	@Override
	public boolean isErrorEnabled() {
		return isEnabled(Level.ERROR);
	}

	@Override
	public void error(String msg) {
		if (!isErrorEnabled()) { return; }
		render(System.err, Level.ERROR, MessageFormatter.format(msg, null));
	}

	@Override
	public void error(String format, Object arg) {
		if (!isErrorEnabled()) { return; }
		render(System.err, Level.ERROR, MessageFormatter.format(format, arg));
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		if (!isErrorEnabled()) { return; }
		render(System.err, Level.ERROR, MessageFormatter.format(format, arg1, arg2));
	}

	@Override
	public void error(String format, Object... arguments) {
		if (!isErrorEnabled()) { return; }
		render(System.err, Level.ERROR, MessageFormatter.arrayFormat(format, arguments));
	}

	@Override
	public void error(String msg, Throwable t) {
		if (!isErrorEnabled()) { return; }
		render(System.err, Level.ERROR, MessageFormatter.format(msg, null, t));
	}
}