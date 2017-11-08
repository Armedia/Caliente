package com.armedia.caliente.tools.pgsql;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.store.CmfStoragePreparationException;
import com.armedia.caliente.store.CmfStorePrep;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.utilities.CfgTools;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.distribution.IVersion;
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

public class PostgresStorePrep implements CmfStorePrep {

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private PostgresConfig config = null;
	private PostgresProcess process = null;
	private CfgTools settings = null;

	@Override
	public void prepareStore(StoreConfiguration cfg, boolean cleanData) throws CmfStoragePreparationException {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			doPrepareStore(cfg, cleanData);
		} catch (Exception e) {
			throw new CmfStoragePreparationException("Failed to prepare the embedded PostgreSQL storage engine", e);
		} finally {
			l.unlock();
		}
	}

	protected void doPrepareStore(StoreConfiguration cfg, boolean cleanData) throws Exception {
		final String dbname = "caliente";
		final String username = "caliente";
		final String password = "caliente";

		final Command cmd = Command.Postgres;
		final IVersion version = Version.Main.V9_6;

		final CfgTools engineSettings = new CfgTools(cfg.getEffectiveSettings());
		String metadataStore = engineSettings.getString("dir.metadata");

		// TODO: Here is where we set where PostgreSQL will be "installed"
		final FixedPath cachedDir = new FixedPath("/path/to/my/extracted/postgres");
		final IPackageResolver packageResolver = new PackagePaths(cmd, cachedDir);
		final IDownloadConfig downloadConfig = new DownloadConfigBuilder().defaultsForCommand(cmd)
			.packageResolver(packageResolver).build();
		final IArtifactStore artifactStore = new CachedArtifactStoreBuilder().defaults(cmd).tempDir(cachedDir)
			.download(downloadConfig).build();
		final IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder().defaults(cmd).artifactStore(artifactStore)
			.build();

		// TODO: Determine where the data will be stored
		final Storage storage = new Storage(dbname, metadataStore);
		if (cleanData) {
			// Clean out the data store
			File storageFile = storage.dbDir();
			if (storageFile.exists() && storageFile.isDirectory()) {
				FileUtils.forceDelete(storageFile);
				FileUtils.forceMkdir(storageFile);
			}
		}

		final PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getInstance(runtimeConfig);
		final Credentials credentials = new Credentials(username, password);
		final PostgresConfig config = new PostgresConfig(version, new Net(), storage, new Timeout(), credentials);
		final Map<String, Object> settings = new TreeMap<>();

		List<String> args = new ArrayList<>();
		// pass info regarding encoding, locale, collate, ctype, instead of setting global
		// environment settings
		args.addAll(
			Arrays.asList("-E", "UTF-8", "--locale=en_US.UTF-8", "--lc-collate=en_US.UTF-8", "--lc-ctype=en_US.UTF-8"));
		// TODO: Calculate a specific, unique port/local address to listen on
		// TODO: Add more arguments for memory size, etc...
		args.addAll(Arrays.asList("-h", "HOSTNAME/IP", "-p", "PORT"));
		config.getAdditionalInitDbParams().addAll(args);
		PostgresExecutable exec = runtime.prepare(config);
		settings.put("jdbc.url", String.format("jdbc:postgresql://%s:%s/%s", config.net().host(), config.net().port(),
			config.storage().dbName()));
		settings.put("jdbc.user", credentials.username());
		settings.put("jdbc.password", credentials.password());
		settings.put("jdbc.driver", "org.postgresql.Driver");
		// TODO: Add more settings such as the base folder, etc...

		this.process = exec.start();
		this.config = config;
		this.settings = new CfgTools(settings);
	}

	public PostgresConfig getConfig() {
		Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.config;
		} finally {
			l.unlock();
		}
	}

	@Override
	public CfgTools getSettings() {
		Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.settings;
		} finally {
			l.unlock();
		}
	}

	@Override
	public void close() {
		Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (this.process != null) {
				this.process.stop();
			}
		} finally {
			this.process = null;
			this.config = null;
			this.settings = null;
			l.unlock();
		}
	}
}