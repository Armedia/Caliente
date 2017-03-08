package com.armedia.caliente.cli.caliente.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.utils.ClasspathPatcher;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.PluggableServiceSelector;
import com.armedia.commons.utilities.Tools;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.store.IArtifactStore;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.PackagePaths;
import ru.yandex.qatools.embed.postgresql.PostgresExecutable;
import ru.yandex.qatools.embed.postgresql.PostgresProcess;
import ru.yandex.qatools.embed.postgresql.PostgresStarter;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.Credentials;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.Net;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.Storage;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.Timeout;
import ru.yandex.qatools.embed.postgresql.config.DownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.distribution.Version;
import ru.yandex.qatools.embed.postgresql.ext.CachedArtifactStoreBuilder;

public class CalienteLauncher extends AbstractLauncher {

	static final Pattern ENGINE_PARSER = Pattern.compile("^\\w+$");
	private static final String MAIN_CLASS = "com.armedia.caliente.cli.caliente.launcher.%s.Caliente_%s";
	private static Properties PARAMETER_PROPERTIES = new Properties();

	public static final String VERSION;

	static {
		String version = null;
		URL url = Thread.currentThread().getContextClassLoader().getResource("version.properties");
		if (url != null) {
			Properties p = new Properties();
			try {
				InputStream in = url.openStream();
				final String str;
				try {
					str = IOUtils.toString(in, Charset.defaultCharset());
				} finally {
					IOUtils.closeQuietly(in);
				}
				p.load(new StringReader(str));
				version = p.getProperty("version");
			} catch (IOException e) {
				version = "(failed to load)";
			}
		}
		VERSION = Tools.coalesce(version, "(unknown)");
	}

	static Properties getParameterProperties() {
		return CalienteLauncher.PARAMETER_PROPERTIES;
	}

	static void PGTest() throws Throwable {
		// define of retrieve db name and credentials
		final String name = "caliente";
		final String username = "caliente";
		final String password = "caliente";

		final Command cmd = Command.Postgres;
		// TODO: Here is where we set where PostgreSQL will be "installed"
		final FixedPath cachedDir = new FixedPath("/path/to/my/extracted/postgres");
		final IPackageResolver packageResolver = new PackagePaths(cmd, cachedDir);
		final IDownloadConfig downloadConfig = new DownloadConfigBuilder().defaultsForCommand(cmd)
			.packageResolver(packageResolver).build();
		final IArtifactStore artifactStore = new CachedArtifactStoreBuilder().defaults(cmd).tempDir(cachedDir)
			.download(downloadConfig).build();
		final IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder().defaults(cmd).artifactStore(artifactStore)
			.build();
		final Storage storage = new Storage(name, "/home/diego/pgtest");
		final PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getInstance(runtimeConfig);
		final PostgresConfig config = new PostgresConfig(Version.Main.PRODUCTION, new Net(), storage, new Timeout(),
			new Credentials(username, password));

		// pass info regarding encoding, locale, collate, ctype, instead of setting global
		// environment settings
		config.getAdditionalInitDbParams().addAll(
			Arrays.asList("-E", "UTF-8", "--locale=en_US.UTF-8", "--lc-collate=en_US.UTF-8", "--lc-ctype=en_US.UTF-8"));
		PostgresExecutable exec = runtime.prepare(config);
		PostgresProcess process = exec.start();

		try {
			// connecting to a running Postgres
			String url = String.format("jdbc:postgresql://%s:%s/%s?currentSchema=public&user=%s&password=%s",
				config.net().host(), config.net().port(), config.storage().dbName(), config.credentials().username(),
				config.credentials().password());

			Connection conn = DriverManager.getConnection(url);

			try {
				// feeding up the database
				conn.createStatement().execute("CREATE TABLE films (code char(5));");
				conn.createStatement().execute("INSERT INTO films VALUES ('movie');");
			} finally {
				conn.close();
			}
		} catch (Throwable t) {
			"".hashCode();
		} finally {
			process.stop();
		}
	}

	public static void main(String[] args) throws Throwable {
		// CalienteLauncher.PGTest();
		// Temporary, for debugging
		System.setProperty("h2.threadDeadlockDetector", "true");
		if (!CLIParam.parse(args)) {
			// If the parameters didn't parse, we fail.
			return;
		}

		// Configure Log4J
		final String mode = CLIParam.mode.getString();
		final String engine = CLIParam.engine.getString();
		if (engine == null) { throw new IllegalArgumentException(String.format("Must provide a --engine parameter")); }
		Matcher m = CalienteLauncher.ENGINE_PARSER.matcher(engine);
		if (!m.matches()) { throw new IllegalArgumentException(
			String.format("Invalid --engine parameter value [%s] - must only contain [a-zA-Z_0-9]", engine)); }

		String logCfg = CLIParam.log_cfg.getString();
		boolean customLog = false;
		if (logCfg != null) {
			final File cfg = new File(logCfg);
			if (cfg.exists() && cfg.isFile() && cfg.canRead()) {
				DOMConfigurator.configureAndWatch(cfg.getCanonicalPath());
				customLog = true;
			}
		}
		if (!customLog) {
			String logName = CLIParam.log.getString();
			if (logName == null) {
				String runTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
				logName = String.format("caliente-%s-%s-%s", engine.toLowerCase(), mode.toLowerCase(), runTime);
			}
			System.setProperty("logName", logName);
			URL config = Thread.currentThread().getContextClassLoader().getResource("log4j.xml");
			if (config != null) {
				DOMConfigurator.configure(config);
			} else {
				config = Thread.currentThread().getContextClassLoader().getResource("log4j.properties");
				if (config != null) {
					PropertyConfigurator.configure(config);
				}
			}
		}

		// Make sure log4j is configured
		Logger.getRootLogger().info("Logging active");
		final Logger console = Logger.getLogger("console");
		console.info(String.format("Launching Caliente v%s %s mode for engine %s%n", CalienteLauncher.VERSION,
			CLIParam.mode.getString(), engine));
		Runtime runtime = Runtime.getRuntime();
		console.info(String.format("Current heap size: %d MB", runtime.totalMemory() / 1024 / 1024));
		console.info(String.format("Maximum heap size: %d MB", runtime.maxMemory() / 1024 / 1024));

		Set<URL> patches = new LinkedHashSet<>();
		PluggableServiceSelector<ClasspathPatcher> selector = new PluggableServiceSelector<ClasspathPatcher>() {
			@Override
			public boolean matches(ClasspathPatcher p) {
				return p.supportsEngine(engine);
			}
		};
		PluggableServiceLocator<ClasspathPatcher> patchers = new PluggableServiceLocator<>(ClasspathPatcher.class,
			selector);
		patchers.setHideErrors(false);
		for (ClasspathPatcher p : patchers) {
			List<URL> l = p.getPatches(engine);
			if ((l == null) || l.isEmpty()) {
				continue;
			}
			for (URL u : l) {
				if (u != null) {
					patches.add(u);
				}
			}
		}

		for (URL u : patches) {
			ClasspathPatcher.addToClassPath(u);
			console.info(String.format("Classpath addition: [%s]", u));
		}

		// Now, convert the command-line parameters into configuration properties
		for (CLIParam p : CLIParam.values()) {
			if (!p.isPresent()) {
				continue;
			}
			List<String> values = p.getAllString();
			if ((values != null) && !values.isEmpty() && (p.property != null)) {
				final String key = p.property.name;
				if ((key != null) && !values.isEmpty()) {
					CalienteLauncher.PARAMETER_PROPERTIES.setProperty(key, StringUtils.join(values, ','));
				}
			}
		}

		// Finally, launch the main class
		// We launch like this because we have to patch the classpath before we link into the rest
		// of the code. If we don't do it like this, the app will refuse to launch altogether
		final Class<?> klass;
		try {
			klass = Class.forName(String.format(CalienteLauncher.MAIN_CLASS, engine, mode));
		} catch (ClassNotFoundException e) {
			System.err.printf("ERROR: Failed to locate a class to support [%s] mode from the [%s] engine", mode,
				engine);
			return;
		}

		final CalienteMain main;
		if (CalienteMain.class.isAssignableFrom(klass)) {
			main = CalienteMain.class.cast(klass.newInstance());
		} else {
			throw new RuntimeException(String.format("Class [%s] is not a valid CalienteMain class", klass.getName()));
		}

		// Lock for single execution
		CmfObjectStore<?, ?> store = main.getObjectStore();
		final boolean writeProperties = (store != null);
		final String pfx = String.format("caliente.%s.%s", engine, mode);
		try {
			if (writeProperties) {
				store.setProperty(String.format("%s.version", pfx), new CmfValue(CalienteLauncher.VERSION));
				store.setProperty(String.format("%s.start", pfx), new CmfValue(new Date()));
			}
			main.run();
		} catch (Throwable t) {
			if (writeProperties) {
				store.setProperty(String.format("%s.error", pfx), new CmfValue(Tools.dumpStackTrace(t)));
			}
			throw new RuntimeException("Execution failed", t);
		} finally {
			// Unlock from single execution
			if (writeProperties) {
				store.setProperty(String.format("%s.end", pfx), new CmfValue(new Date()));
			}
		}
	}
}