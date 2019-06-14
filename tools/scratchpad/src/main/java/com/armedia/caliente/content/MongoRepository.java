package com.armedia.caliente.content;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import javax.jcr.Repository;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.document.mongo.MongoDocumentNodeStoreBuilder;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexProvider;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.function.CheckedSupplier;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class MongoRepository extends BaseShareableLockable implements CheckedSupplier<Repository, Exception> {

	private final Logger console = LoggerFactory.getLogger(getClass());

	private static final String DEFAULT_DATABASE = "oakTest";
	private static final String DEFAULT_USER = "oakTest";
	private static final String DEFAULT_PASSWORD = "oakTest";
	private static final ServerAddress DEFAULT_SERVER_ADDRESS = new ServerAddress();
	private static final InetSocketAddress DEFAULT_SOCKET_ADDRESS = MongoRepository.DEFAULT_SERVER_ADDRESS
		.getSocketAddress();
	private static final MongoCredential DEFAULT_CREDENTIAL = MongoCredential.createCredential(
		MongoRepository.DEFAULT_USER, MongoRepository.DEFAULT_DATABASE, MongoRepository.DEFAULT_PASSWORD.toCharArray());

	private ServerAddress serverAddress = MongoRepository.DEFAULT_SERVER_ADDRESS;
	private MongoCredential credential = MongoRepository.DEFAULT_CREDENTIAL;

	public MongoRepository setServerAddress(ServerAddress serverAddress) {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.serverAddress = Tools.coalesce(serverAddress, MongoRepository.DEFAULT_SERVER_ADDRESS);
			return this;
		}
	}

	private int getDesiredPort(int port) {
		if (port > 0) { return port; }
		if (port == 0) { return this.serverAddress.getPort(); }
		return ServerAddress.defaultPort();
	}

	public ServerAddress getServerAddress() {
		return shareLocked(() -> this.serverAddress);
	}

	public MongoRepository setHost(String host) {
		return setHost(host, 0);
	}

	public MongoRepository setHost(String host, int port) {
		try (MutexAutoLock lock = autoMutexLock()) {
			host = Tools.coalesce(host, MongoRepository.DEFAULT_SERVER_ADDRESS.getHost());
			this.serverAddress = new ServerAddress(host, getDesiredPort(port));
			return this;
		}
	}

	public String getHost() {
		return shareLocked(() -> this.serverAddress.getHost());
	}

	public MongoRepository setPort(int port) {
		return setHost(null, port);
	}

	public int getPort() {
		return shareLocked(() -> this.serverAddress.getPort());
	}

	public MongoRepository setInetAddress(InetAddress address) {
		return setSocketAddress(address, 0);
	}

	public MongoRepository setSocketAddress(InetAddress address, int port) {
		try (MutexAutoLock lock = autoMutexLock()) {
			port = getDesiredPort(port);
			address = Tools.coalesce(address, MongoRepository.DEFAULT_SOCKET_ADDRESS.getAddress());
			this.serverAddress = new ServerAddress(address, port);
			return this;
		}
	}

	public MongoRepository setSocketAddress(InetSocketAddress address) {
		try (MutexAutoLock lock = autoMutexLock()) {
			address = Tools.coalesce(address, MongoRepository.DEFAULT_SOCKET_ADDRESS);
			this.serverAddress = new ServerAddress(address);
			return this;
		}
	}

	public InetSocketAddress getSocketAddress() {
		return shareLocked(() -> this.serverAddress.getSocketAddress());
	}

	public List<InetSocketAddress> getSocketAddresses() {
		return shareLocked(() -> this.serverAddress.getSocketAddresses());
	}

	public MongoRepository setCredential(MongoCredential credential) {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.credential = Tools.coalesce(credential, MongoRepository.DEFAULT_CREDENTIAL);
			return this;
		}
	}

	public MongoCredential getCredential() {
		return shareLocked(() -> this.credential);
	}

	@Override
	public Repository getChecked() throws Exception {
		final String user = this.credential.getUserName();
		final String db = this.credential.getSource();
		this.console.info("Initializing the repository connection to {}@{} as {}", db, this.serverAddress, user);
		//
		;
		MongoClientOptions.Builder options = new MongoClientOptions.Builder() //
		//
		;
		this.console.info("Readying the Mongo client");
		MongoClient client = new MongoClient(this.serverAddress, this.credential, options.build()) //
		//
		;

		this.console.info("Mongo client ready!! Creating the NodeStore...");
		MongoDocumentNodeStoreBuilder builder = new MongoDocumentNodeStoreBuilder() //
			.setMongoDB(client, db) //
		//
		;

		// Configure oak
		this.console.info("NodeStore ready!! Creating the Oak instance...");
		Oak oak = new Oak(builder.build()) //
			.with(new PropertyIndexProvider()) //
		//
		;

		// Configure jcr
		this.console.info("Oak ready!! Creating the Jcr instance...");
		Jcr jcr = new Jcr(oak) //
			.with(new OpenSecurityProvider()) //
		//
		;

		// Return the repository
		this.console.info("Jcr ready!! Creating the Repository...");
		Repository repository = jcr.createRepository();
		this.console.info("Repository ready!");
		return repository;
	}
}