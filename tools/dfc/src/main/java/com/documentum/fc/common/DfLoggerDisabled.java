/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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

/**
 * This class has a "Disabled" suffix to mark it as such. If there's a desire to dump Log4J entirely
 * and stick to only Logback, remove the "Disabled" suffix.
 *
 *
 *
 */

@Visibility(visibility = VisibilityType.PUBLIC)
public final class DfLoggerDisabled {
	private static final String MUTE = "MUTE";
	private static final String NULL_CATEGORY = "null";
	private static final String FQCN = DfLoggerDisabled.class.getName() + ".";
	private static final String TRACING = "tracing.";
	private static final int TRACING_LENGTH = DfLoggerDisabled.TRACING.length();
	private static ThreadLocal<Stack<String>> s_prefixes = new ThreadLocal<>();
	private static ThreadLocal<Integer> s_muteCounter = new ThreadLocal<>();
	private static final Loggers s_loggers = new Loggers();

	private static Throwable optionallyGetLogStack(Level level, Throwable givenThrowable) {
		if (level.isGreaterOrEqual(LoggingConfigurator.getLevelToForceStack())) {
			if (givenThrowable != null) { return new ThrowableStack(2, givenThrowable); }
			return new ThrowableStack(2);
		}
		return givenThrowable;
	}

	public static void fatal(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.FATAL)) {
			logger.log(DfLoggerDisabled.FQCN, Level.FATAL, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.FATAL, t));
		}
	}

	public static void fatal(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.FATAL)) {
			logger.log(DfLoggerDisabled.FQCN, Level.FATAL, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.FATAL, t));
		}
	}

	public static void error(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.ERROR)) {
			logger.log(DfLoggerDisabled.FQCN, Level.ERROR, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.ERROR, t));
		}
	}

	public static void error(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.ERROR)) {
			logger.log(DfLoggerDisabled.FQCN, Level.ERROR, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.ERROR, t));
		}
	}

	public static void warn(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.WARN)) {
			logger.log(DfLoggerDisabled.FQCN, Level.WARN, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.WARN, t));
		}
	}

	public static void warn(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.WARN)) {
			logger.log(DfLoggerDisabled.FQCN, Level.WARN, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.WARN, t));
		}
	}

	public static void info(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.INFO)) {
			logger.log(DfLoggerDisabled.FQCN, Level.INFO, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.INFO, t));
		}
	}

	public static void info(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.INFO)) {
			logger.log(DfLoggerDisabled.FQCN, Level.INFO, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.INFO, t));
		}
	}

	public static void debug(Object source, String message, Object[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.DEBUG)) {
			logger.log(DfLoggerDisabled.FQCN, Level.DEBUG, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.DEBUG, t));
		}
	}

	public static void debug(Object source, String message, String[] params, Throwable t) {
		Logger logger = DfLoggerDisabled.getLogger(source);
		if (logger.isEnabledFor(Level.DEBUG)) {
			logger.log(DfLoggerDisabled.FQCN, Level.DEBUG, DfLoggerDisabled.getFullMessage(message, params),
				DfLoggerDisabled.optionallyGetLogStack(Level.DEBUG, t));
		}
	}

	public static void trace(Object source, String message, Object[] params, Throwable t) {
		DfLoggerDisabled.getLoggerForTrace(source).log(DfLoggerDisabled.FQCN, Level.DEBUG,
			DfLoggerDisabled.getFullMessage(message, params), DfLoggerDisabled.optionallyGetLogStack(Level.DEBUG, t));
	}

	public static void trace(Object source, String message, String[] params, Throwable t) {
		DfLoggerDisabled.getLoggerForTrace(source).log(DfLoggerDisabled.FQCN, Level.DEBUG,
			DfLoggerDisabled.getFullMessage(message, params), DfLoggerDisabled.optionallyGetLogStack(Level.DEBUG, t));
	}

	public static boolean isFatalEnabled(Object source) {
		return DfLoggerDisabled.getLogger(source).isEnabledFor(Level.FATAL);
	}

	public static boolean isErrorEnabled(Object source) {
		return DfLoggerDisabled.getLogger(source).isEnabledFor(Level.ERROR);
	}

	public static boolean isWarnEnabled(Object source) {
		return DfLoggerDisabled.getLogger(source).isEnabledFor(Level.WARN);
	}

	public static boolean isInfoEnabled(Object source) {
		return DfLoggerDisabled.getLogger(source).isEnabledFor(Level.INFO);
	}

	public static boolean isDebugEnabled(Object source) {
		return DfLoggerDisabled.getLogger(source).isEnabledFor(Level.DEBUG);
	}

	public static boolean isTraceEnabled(Object source) {
		return DfLoggerDisabled.getLoggerForTrace(source).isEnabledFor(Level.DEBUG);
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
					categoryObj = DfLoggerDisabled.NULL_CATEGORY;
				}
			}
		}
		Map<Object, Logger> loggersMap = DfLoggerDisabled.s_loggers.getLoggersMap();
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
						coreCategory = DfLoggerDisabled.NULL_CATEGORY;
					}
				}
			}
			String categoryWithPrefix = coreCategory;
			String prefix = DfLoggerDisabled.getCurrentPrefix();
			if (prefix != null) {
				categoryWithPrefix = prefix + '.' + coreCategory;
			}
			logger = Logger.getLogger(categoryWithPrefix);
			loggersMap.put(categoryObj, logger);
		}
		return logger;
	}

	public static Logger getRootLogger() {
		String prefix = DfLoggerDisabled.getCurrentPrefix();
		if (prefix == null) { return Logger.getRootLogger(); }
		return Logger.getLogger(prefix);
	}

	public static void setClientContext(String prefix) {
		DfLoggerDisabled.s_loggers.reset();

		Stack<String> prefixesStack = DfLoggerDisabled.s_prefixes.get();
		if (prefixesStack == null) {
			prefixesStack = new Stack<>();
			DfLoggerDisabled.s_prefixes.set(prefixesStack);
		}
		prefixesStack.push(prefix);
	}

	public static void restoreClientContext() {
		Stack<String> prefixesStack = DfLoggerDisabled.s_prefixes.get();
		if ((prefixesStack != null) && (!prefixesStack.empty())) {
			prefixesStack.pop();
			DfLoggerDisabled.s_loggers.reset();
		}
	}

	public static void mute(boolean fMode) {
		Integer mute = DfLoggerDisabled.s_muteCounter.get();
		if ((mute == null) && (!fMode)) { return; }
		DfLoggerDisabled.s_loggers.reset();

		int muteCounter = mute != null ? mute.intValue() : 0;
		if (fMode) {
			DfLoggerDisabled.s_muteCounter.set(Integer.valueOf(muteCounter + 1));
		} else {
			muteCounter--;
			if (muteCounter > 0) {
				DfLoggerDisabled.s_muteCounter.set(Integer.valueOf(muteCounter + 1));
			} else {
				DfLoggerDisabled.s_muteCounter.set(null);
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
					coreCategory = DfLoggerDisabled.NULL_CATEGORY;
				}
			}
		}
		StringBuilder sb = new StringBuilder(DfLoggerDisabled.TRACING_LENGTH + coreCategory.length());
		sb.append(DfLoggerDisabled.TRACING).append(coreCategory);
		String category = sb.toString();

		return Logger.getLogger(category);
	}

	private static String getCurrentPrefix() {
		if (DfLoggerDisabled.s_muteCounter.get() != null) { return DfLoggerDisabled.MUTE; }
		Stack<String> prefixesStack = DfLoggerDisabled.s_prefixes.get();
		if ((prefixesStack == null) || (prefixesStack.empty())) { return null; }
		return prefixesStack.peek();
	}

	private static final class Loggers {
		static final int CACHE_SIZE = 4999;

		public Map<Object, Logger> getLoggersMap() {
			Map<Object, Logger> map = this.m_loggersMap.get();
			if (map == null) {
				map = new HashMap<>(Loggers.CACHE_SIZE);
				this.m_loggersMap.set(map);
			}
			return map;
		}

		public void reset() {
			this.m_loggersMap.set(new HashMap<Object, Logger>(Loggers.CACHE_SIZE));
		}

		private final ThreadLocal<Map<Object, Logger>> m_loggersMap = new ThreadLocal<>();
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
		// Factory localFactory = new Factory("DfLoggerDisabled.java", DfLoggerDisabled.class);

		// Logger.getRootLogger().setResourceBundle(new LoggingResourceBundle());
		DfLoggerDisabled.registerResourceBundle(DfcMessages.getResourceBundle());
		// LoggingConfigurator.performInitialConfiguration();
	}
}