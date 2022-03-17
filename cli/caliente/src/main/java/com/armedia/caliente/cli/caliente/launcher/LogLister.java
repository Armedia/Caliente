package com.armedia.caliente.cli.caliente.launcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.iterators.EnumerationIterator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.armedia.commons.utilities.StreamTools;
import com.armedia.commons.utilities.Tools;

/**
 * This class facilitates the listing of active Log4J {@link Logger} instances in the system.
 *
 * @author diego
 */
public class LogLister {

	/**
	 * This enum classifies the Log4J into 3 basic types: class-level logger (named after a
	 * classname), package-level loggers (named after a package), and named loggers (with a name
	 * that matches neither a classname nor a package name).
	 *
	 * @author diego
	 */
	public static enum LogType {
		//
		CLASS, //
		PACKAGE, //
		NAMED, //
		//
		;

		public static LogType classify(Logger l) {
			String name = l.getName();
			if (name != null) {
				try {
					Class.forName(name);
					return LogType.CLASS;
				} catch (ClassNotFoundException e) {
				}

				Package p = Package.getPackage(name);
				if (p != null) { return LogType.PACKAGE; }
			}

			return LogType.NAMED;
		}
	}

	/**
	 * This class provides a simplified mapping for the log levels from Log4J, to include one
	 * additional level called "DELEGATE", which simply delegates its logging priority filter to its
	 * parent logger.
	 *
	 * @author diego
	 */
	public static enum LogLevel {
		//
		// DELEGATE(null), //
		OFF(Level.OFF), //
		ALL(Level.ALL), //
		TRACE(Level.TRACE), //
		DEBUG(Level.DEBUG), //
		INFO(Level.INFO), //
		WARN(Level.WARN), //
		ERROR(Level.ERROR), //
		FATAL(Level.FATAL), //
		//
		;

		private final Level level;

		private LogLevel(Level level) {
			this.level = level;
		}

		public Level getLevel() {
			return this.level;
		}

		public boolean isGreaterOrEqual(LogLevel level) {
			return isGReaterOrEqual(level != null ? level.getLevel() : null);
		}

		public boolean isGReaterOrEqual(Level level) {
			return this.level.isGreaterOrEqual(level);
		}

		private static final Map<Level, LogLevel> MAP;
		static {
			Map<Level, LogLevel> map = new LinkedHashMap<>();
			for (LogLevel l : LogLevel.values()) {
				map.put(l.level, l);
			}
			MAP = Tools.freezeMap(map);
		}

		public static LogLevel decode(Level level) {
			LogLevel l = LogLevel.MAP.get(level);
			if (l == null) { throw new IllegalArgumentException(String.format("Unsupported log level %s", level)); }
			return l;
		}
	}

	public static Set<LogType> getLogTypes() {
		return EnumSet.allOf(LogType.class);
	}

	public static Set<LogLevel> getLogLevels() {
		return EnumSet.allOf(LogLevel.class);
	}

	private static Predicate<Logger> renderLogTypePredicate(Collection<LogType> types) {
		final Predicate<Logger> p;
		if ((types == null) || types.isEmpty()) {
			p = null;
		} else {
			final Set<LogType> finalTypes = EnumSet.noneOf(LogType.class);
			finalTypes.stream().filter(Objects::nonNull).forEach(finalTypes::add);
			p = (l) -> finalTypes.contains(LogType.classify(l));
		}
		return p;
	}

	private static Collection<LogType> renderLogTypeCollection(LogType... types) {
		Collection<LogType> c = null;
		if (types.length > 0) {
			c = Arrays.asList(types);
		}
		return c;
	}

	/**
	 * List all the {@link Logger} instances matching the given types.
	 *
	 * @param types
	 */
	public static Collection<Logger> getLoggers(LogType... types) {
		return LogLister.getLoggers(LogLister.renderLogTypeCollection(types));
	}

	/**
	 * List all the {@link Logger} instances matching the given types. If the collection is
	 * {@code null} or empty, it will match all loggers.
	 *
	 * @param types
	 */
	public static Collection<Logger> getLoggers(Collection<LogType> types) {
		return LogLister.getLoggers(LogLister.renderLogTypePredicate(types));
	}

	/**
	 * List all the {@link Logger} instances matching the given predicate. If the predicate is
	 * {@code null}, it will match all Loggers.
	 *
	 * @param predicate
	 */
	public static Collection<Logger> getLoggers(Predicate<Logger> predicate) {
		return LogLister.loggers(predicate) //
			.collect(Collectors.toCollection(LinkedList::new)) //
		;
	}

	public static Stream<Logger> loggers(LogType... types) {
		return LogLister.loggers(LogLister.renderLogTypeCollection(types));
	}

	public static Stream<Logger> loggers(Collection<LogType> types) {
		return LogLister.loggers(LogLister.renderLogTypePredicate(types));
	}

	/**
	 * Returns a stream for all the {@link Logger} instances matching the given predicate. If the
	 * predicate is {@code null}, it will match all Loggers.
	 *
	 * @param predicate
	 */
	public static Stream<Logger> loggers(Predicate<Logger> predicate) {
		final Enumeration<?> e = Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
		Stream<Logger> s = StreamTools.of(new EnumerationIterator<>(e)) //
			.filter(Logger.class::isInstance) //
			.map(Logger.class::cast) //
		;
		if (predicate != null) {
			s = s.filter(predicate);
		}
		return s;
	}
}