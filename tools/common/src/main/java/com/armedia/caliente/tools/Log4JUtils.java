package com.armedia.caliente.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.iterators.EnumerationIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.armedia.commons.utilities.StreamTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.ShareableLockable;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

/**
 * This class facilitates the listing of active Log4J {@link Logger} instances in the system.
 *
 * @author diego
 */
public class Log4JUtils {

	public static final Pattern LOG_INFO = Pattern.compile(
		"^(.+)(?:=(OFF|ALL|TRACE|DEBUG|INFO|WARN|ERROR|FATAL|[1-9][0-9]*))(?:,(1|0|t(?:rue)?|f(?:alse)?|y(?:es)|n(?:o)|on|off))?$",
		Pattern.CASE_INSENSITIVE);

	public static class LoggerSettings implements Comparable<LoggerSettings> {
		private final String name;
		private final Level level;
		private final Boolean additive;
		private final String string;

		public LoggerSettings(String info) {
			Matcher m = Log4JUtils.LOG_INFO
				.matcher(Objects.requireNonNull(info, "Must provide a non-null log information string"));
			if (!m.matches()) {
				throw new IllegalArgumentException(
					"The string [" + info + "] doesn't match the required format for log information");
			}

			this.name = m.group(1);

			Level newLevel = null;
			try {
				newLevel = Level.toLevel(Integer.valueOf(m.group(2)));
			} catch (NumberFormatException e) {
				newLevel = Level.toLevel(m.group(2));
			}
			this.level = newLevel;

			String additivity = m.group(3);
			if (StringUtils.isEmpty(additivity)) {
				this.additive = null;
			} else {
				this.additive = Tools.toBoolean(additivity);
			}
			this.string = String.format("%s=%s%s", this.name, this.level,
				(this.additive != null ? "," + this.additive : ""));
		}

		public LoggerSettings(String name, Level level) {
			this(name, level, null);
		}

		/**
		 * @param name
		 * @param level
		 * @param additive
		 */
		public LoggerSettings(String name, Level level, Boolean additive) {
			this.name = name;
			this.level = level;
			this.additive = additive;
			this.string = String.format("%s=%s%s", this.name, this.level,
				(this.additive != null ? "," + this.additive : ""));
		}

		public boolean apply() {
			Logger logger = null;
			if (StringUtils.equalsIgnoreCase("root", this.name)) {
				logger = org.apache.log4j.Logger.getRootLogger();
			} else {
				logger = org.apache.log4j.Logger.getLogger(this.name);
			}
			boolean changed = false;
			if (!Objects.equals(logger.getLevel(), this.level)) {
				logger.setLevel(this.level);
				changed = true;
			}
			if ((this.additive != null) && (logger.getAdditivity() != this.additive)) {
				logger.setAdditivity(this.additive);
				changed = true;
			}
			return changed;
		}

		public String getName() {
			return this.name;
		}

		public Level getLevel() {
			return this.level;
		}

		public boolean isAdditive() {
			return this.additive;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.name, this.level, this.additive);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			LoggerSettings other = LoggerSettings.class.cast(obj);
			return (compareTo(other) == 0);
		}

		@Override
		public int compareTo(LoggerSettings o) {
			if (o == null) { return 1; }
			int r = 0;
			r = Tools.compare(this.name, o.name);
			if (r != 0) { return r; }
			r = Tools.compare(this.level.toInt(), o.level.toInt());
			if (r != 0) { return r; }
			r = Tools.compare(this.additive, o.additive);
			if (r != 0) { return r; }
			return 0;
		}

		@Override
		public String toString() {
			return this.string;
		}
	}

	private static final Collection<LoggerSettings> NO_SETTINGS = Collections.emptyList();
	private static final ShareableLockable logConfiguratorLock = new BaseShareableLockable();
	private static volatile BooleanSupplier logConfigurator = null;

	public static void setCustomLogs(Collection<LoggerSettings> settings) {
		try (SharedAutoLock shared = Log4JUtils.logConfiguratorLock.sharedAutoLock()) {
			if (Log4JUtils.logConfigurator != null) { return; }
			try (MutexAutoLock mutex = shared.upgrade()) {
				final Collection<LoggerSettings> newSettings = Tools.coalesce(settings, Log4JUtils.NO_SETTINGS);
				Log4JUtils.logConfigurator = () -> {
					boolean changed = false;
					for (LoggerSettings s : newSettings) {
						changed |= s.apply();
					}
					return changed;
				};
			}
		}
	}

	public static boolean apply() {
		try (SharedAutoLock shared = Log4JUtils.logConfiguratorLock.sharedAutoLock()) {
			if (Log4JUtils.logConfigurator == null) { return false; }
			return Log4JUtils.logConfigurator.getAsBoolean();
		}
	}

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
		return Log4JUtils.getLoggers(Log4JUtils.renderLogTypeCollection(types));
	}

	/**
	 * List all the {@link Logger} instances matching the given types. If the collection is
	 * {@code null} or empty, it will match all loggers.
	 *
	 * @param types
	 */
	public static Collection<Logger> getLoggers(Collection<LogType> types) {
		return Log4JUtils.getLoggers(Log4JUtils.renderLogTypePredicate(types));
	}

	/**
	 * List all the {@link Logger} instances matching the given predicate. If the predicate is
	 * {@code null}, it will match all Loggers.
	 *
	 * @param predicate
	 */
	public static Collection<Logger> getLoggers(Predicate<Logger> predicate) {
		return Log4JUtils.loggers(predicate) //
			.collect(Collectors.toCollection(LinkedList::new)) //
		;
	}

	public static Stream<Logger> loggers(LogType... types) {
		return Log4JUtils.loggers(Log4JUtils.renderLogTypeCollection(types));
	}

	public static Stream<Logger> loggers(Collection<LogType> types) {
		return Log4JUtils.loggers(Log4JUtils.renderLogTypePredicate(types));
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