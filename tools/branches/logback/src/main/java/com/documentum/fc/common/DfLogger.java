package com.documentum.fc.common;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Stack;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.documentum.fc.common.impl.GlobalResourceBundle;
import com.documentum.fc.common.impl.documentation.Visibility;
import com.documentum.fc.common.impl.documentation.VisibilityType;
import com.documentum.fc.common.impl.logging.LoggingConfigurator;
import com.documentum.fc.impl.util.ThrowableStack;

@Visibility(visibility = VisibilityType.PUBLIC)
public final class DfLogger {
	private static final String MUTE = "MUTE";
	private static final String NULL_CATEGORY = "null";
	private static final String FQCN = DfLogger.class.getName() + ".";
	private static final String TRACING = "tracing.";
	private static final int TRACING_LENGTH = DfLogger.TRACING.length();
	private static ThreadLocal<Stack<String>> s_prefixes = new ThreadLocal<Stack<String>>();
	private static ThreadLocal<Integer> s_muteCounter = new ThreadLocal<Integer>();
	private static final Loggers s_loggers = new Loggers();

	private static Throwable optionallyGetLogStack(Level level, Throwable givenThrowable) {
		if (level.isGreaterOrEqual(LoggingConfigurator.getLevelToForceStack())) {
			if (givenThrowable != null) { return new ThrowableStack(2, givenThrowable); }
			return new ThrowableStack(2);
		}
		return givenThrowable;
	}

	public static void fatal(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.FATAL)) {
			logger.log(DfLogger.FQCN, Level.FATAL, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.FATAL, t));
		}
	}

	public static void fatal(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.FATAL)) {
			logger.log(DfLogger.FQCN, Level.FATAL, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.FATAL, t));
		}
	}

	public static void error(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.ERROR)) {
			logger.log(DfLogger.FQCN, Level.ERROR, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.ERROR, t));
		}
	}

	public static void error(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.ERROR)) {
			logger.log(DfLogger.FQCN, Level.ERROR, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.ERROR, t));
		}
	}

	public static void warn(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.WARN)) {
			logger.log(DfLogger.FQCN, Level.WARN, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.WARN, t));
		}
	}

	public static void warn(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.WARN)) {
			logger.log(DfLogger.FQCN, Level.WARN, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.WARN, t));
		}
	}

	public static void info(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.INFO)) {
			logger.log(DfLogger.FQCN, Level.INFO, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.INFO, t));
		}
	}

	public static void info(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.INFO)) {
			logger.log(DfLogger.FQCN, Level.INFO, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.INFO, t));
		}
	}

	public static void debug(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.DEBUG)) {
			logger.log(DfLogger.FQCN, Level.DEBUG, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.DEBUG, t));
		}
	}

	public static void debug(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLogger.getLogger(source);
		if (logger.isEnabledFor(Level.DEBUG)) {
			logger.log(DfLogger.FQCN, Level.DEBUG, DfLogger.getFullMessage(message, params),
				DfLogger.optionallyGetLogStack(Level.DEBUG, t));
		}
	}

	public static void trace(Object source, String message, Object[] params, Throwable t) {
		DfLogger.getLoggerForTrace(source).log(DfLogger.FQCN, Level.DEBUG, DfLogger.getFullMessage(message, params),
			DfLogger.optionallyGetLogStack(Level.DEBUG, t));
	}

	public static void trace(Object source, String message, String[] params, Throwable t) {
		DfLogger.getLoggerForTrace(source).log(DfLogger.FQCN, Level.DEBUG, DfLogger.getFullMessage(message, params),
			DfLogger.optionallyGetLogStack(Level.DEBUG, t));
	}

	public static boolean isFatalEnabled(Object source) {
		return DfLogger.getLogger(source).isEnabledFor(Level.FATAL);
	}

	public static boolean isErrorEnabled(Object source) {
		return DfLogger.getLogger(source).isEnabledFor(Level.ERROR);
	}

	public static boolean isWarnEnabled(Object source) {
		return DfLogger.getLogger(source).isEnabledFor(Level.WARN);
	}

	public static boolean isInfoEnabled(Object source) {
		return DfLogger.getLogger(source).isEnabledFor(Level.INFO);
	}

	public static boolean isDebugEnabled(Object source) {
		return DfLogger.getLogger(source).isEnabledFor(Level.DEBUG);
	}

	public static boolean isTraceEnabled(Object source) {
		return DfLogger.getLoggerForTrace(source).isEnabledFor(Level.DEBUG);
	}

	public static Logger getLogger(Object source) {
		Object categoryObj;
		if ((source instanceof String)) {
			categoryObj = source;
		} else {
			if ((source instanceof Class)) {
				categoryObj = source;
			} else {
				if (source != null) {
					categoryObj = source.getClass();
				} else {
					categoryObj = DfLogger.NULL_CATEGORY;
				}
			}
		}
		Map<Object, Logger> loggersMap = DfLogger.s_loggers.getLoggersMap();
		Logger logger = loggersMap.get(categoryObj);
		if (logger == null) {
			String coreCategory;
			if ((source instanceof String)) {
				coreCategory = (String) source;
			} else {
				if ((source instanceof Class)) {
					coreCategory = Class.class.cast(source).getName();
				} else {
					if (source != null) {
						coreCategory = source.getClass().getName();
					} else {
						coreCategory = DfLogger.NULL_CATEGORY;
					}
				}
			}
			String categoryWithPrefix = coreCategory;
			String prefix = DfLogger.getCurrentPrefix();
			if (prefix != null) {
				categoryWithPrefix = prefix + '.' + coreCategory;
			}
			logger = Logger.getLogger(categoryWithPrefix);
			loggersMap.put(categoryObj, logger);
		}
		return logger;
	}

	public static Logger getRootLogger() {
		String prefix = DfLogger.getCurrentPrefix();
		if (prefix == null) { return Logger.getRootLogger(); }
		return Logger.getLogger(prefix);
	}

	public static void setClientContext(String prefix) {
		DfLogger.s_loggers.reset();

		Stack<String> prefixesStack = DfLogger.s_prefixes.get();
		if (prefixesStack == null) {
			prefixesStack = new Stack<String>();
			DfLogger.s_prefixes.set(prefixesStack);
		}
		prefixesStack.push(prefix);
	}

	public static void restoreClientContext() {
		Stack<String> prefixesStack = DfLogger.s_prefixes.get();
		if ((prefixesStack != null) && (!prefixesStack.empty())) {
			prefixesStack.pop();
			DfLogger.s_loggers.reset();
		}
	}

	public static void mute(boolean fMode) {
		Integer mute = DfLogger.s_muteCounter.get();
		if ((mute == null) && (!fMode)) { return; }
		DfLogger.s_loggers.reset();

		int muteCounter = mute != null ? mute.intValue() : 0;
		if (fMode) {
			DfLogger.s_muteCounter.set(Integer.valueOf(muteCounter + 1));
		} else {
			muteCounter--;
			if (muteCounter > 0) {
				DfLogger.s_muteCounter.set(Integer.valueOf(muteCounter + 1));
			} else {
				DfLogger.s_muteCounter.set(null);
			}
		}
	}

	public static void registerResourceBundle(ResourceBundle bundle) {
		GlobalResourceBundle.getInstance().add(bundle);
	}

	public static String getFullMessage(String message, String[] params) {
		String fullMessage = (String) new LoggingResourceBundle().getObject(message);
		if (fullMessage != null) {
			if (params != null) {
				fullMessage = MessageFormat.format(fullMessage, (Object[]) params);
			}
		} else {
			fullMessage = message;
		}
		return fullMessage;
	}

	public static String getFullMessage(String message, Object[] params) {
		String fullMessage = (String) new LoggingResourceBundle().getObject(message);
		if (fullMessage != null) {
			if (params != null) {
				fullMessage = MessageFormat.format(fullMessage, params);
			}
		} else {
			fullMessage = message;
		}
		return fullMessage;
	}

	private static Logger getLoggerForTrace(Object source) {
		String coreCategory;
		if ((source instanceof String)) {
			coreCategory = (String) source;
		} else {
			if ((source instanceof Class)) {
				coreCategory = Class.class.cast(source).getName();
			} else {
				if (source != null) {
					coreCategory = source.getClass().getName();
				} else {
					coreCategory = DfLogger.NULL_CATEGORY;
				}
			}
		}
		StringBuilder sb = new StringBuilder(DfLogger.TRACING_LENGTH + coreCategory.length());
		sb.append(DfLogger.TRACING).append(coreCategory);
		String category = sb.toString();

		return Logger.getLogger(category);
	}

	private static String getCurrentPrefix() {
		if (DfLogger.s_muteCounter.get() != null) { return DfLogger.MUTE; }
		Stack<String> prefixesStack = DfLogger.s_prefixes.get();
		if ((prefixesStack == null) || (prefixesStack.empty())) { return null; }
		return prefixesStack.peek();
	}

	private static final class Loggers {
		static final int CACHE_SIZE = 4999;

		public Map<Object, Logger> getLoggersMap() {
			Map<Object, Logger> map = this.m_loggersMap.get();
			if (map == null) {
				map = new HashMap<Object, Logger>(Loggers.CACHE_SIZE);
				this.m_loggersMap.set(map);
			}
			return map;
		}

		public void reset() {
			this.m_loggersMap.set(new HashMap<Object, Logger>(Loggers.CACHE_SIZE));
		}

		private final ThreadLocal<Map<Object, Logger>> m_loggersMap = new ThreadLocal<Map<Object, Logger>>();
	}

	private static class LoggingResourceBundle extends ResourceBundle {
		@Override
		public synchronized Enumeration<String> getKeys() {
			return GlobalResourceBundle.getInstance().getKeys();
		}

		@Override
		protected synchronized Object handleGetObject(String key) {
			try {
				return "[" + key + "] " + GlobalResourceBundle.getInstance().getObject(key);
			} catch (MissingResourceException e) {
			}
			return key;
		}
	}

	static {
		// Factory localFactory = new Factory("DfLogger.java", DfLogger.class);

		// Logger.getRootLogger().setResourceBundle(new LoggingResourceBundle());
		DfLogger.registerResourceBundle(DfcMessages.getResourceBundle());
		// LoggingConfigurator.performInitialConfiguration();
	}
}