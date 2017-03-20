package com.armedia.caliente.tools.pgsql;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.armedia.caliente.store.CmfStoragePreparationException;
import com.armedia.caliente.store.CmfStorePrep;
import com.armedia.caliente.store.xml.StoreConfiguration;

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

	private PostgresConfig config = null;
	private PostgresProcess process = null;

	@Override
	public void prepareStore(StoreConfiguration cfg, boolean cleanData) throws CmfStoragePreparationException {
		try {
			doPrepareStore(cfg, cleanData);
		} catch (Exception e) {
			throw new CmfStoragePreparationException("Failed to prepare the embedded PostgreSQL storage engine", e);
		}
	}

	protected void doPrepareStore(StoreConfiguration cfg, boolean cleanData) throws Exception {
		// define of retrieve db name and credentials
		final String name = "caliente";
		final String username = "caliente";
		final String password = "caliente";

		final Command cmd = Command.Postgres;
		final IVersion version = Version.Main.V9_6;
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
		final PostgresConfig config = new PostgresConfig(version, new Net(), storage, new Timeout(),
			new Credentials(username, password));

		// pass info regarding encoding, locale, collate, ctype, instead of setting global
		// environment settings
		config.args(); // TODO: Add more arguments for memory size, etc...
		config.getAdditionalInitDbParams().addAll(
			Arrays.asList("-E", "UTF-8", "--locale=en_US.UTF-8", "--lc-collate=en_US.UTF-8", "--lc-ctype=en_US.UTF-8"));
		PostgresExecutable exec = runtime.prepare(config);
		this.process = exec.start();
		this.config = config;
	}

	protected String getStoreURIString() {
		return String.format("jdbc:postgresql://%s:%s/%s", this.config.net().host(), this.config.net().port(),
			this.config.storage().dbName());
	}

	@Override
	public URI getStoreURI() {
		try {
			return new URI(getStoreURIString());
		} catch (URISyntaxException e) {
			throw new RuntimeException("URI Syntax is invalid");
		}
	}

	@Override
	public void close() {
		try {
			if (this.process != null) {
				this.process.stop();
			}
		} finally {
			this.process = null;
			this.config = null;
		}
	}
}