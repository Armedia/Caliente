package com.armedia.caliente.content;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.function.LazySupplier;

public class ContentStoreClient {
	// TODO: Fix this for something meaningful and reproducible ... maybe based on IP addresses?
	private static final LazySupplier<String> ID = new LazySupplier<>(ContentStoreClient::calculateClientId);

	private static String calculateClientId() {
		return UUID.randomUUID().toString();
	}

	private final String applicationName;
	private final String id;

	private final AtomicLong fileId = new AtomicLong(0);

	public ContentStoreClient(String applicationName) {
		this(applicationName, null);
	}

	public ContentStoreClient(String applicationName, String id) {
		this.applicationName = applicationName;
		if (StringUtils.isEmpty(id)) {
			id = ContentStoreClient.ID.get();
		}
		this.id = id;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public String getId() {
		return this.id;
	}

	public long getNextFileId() {
		return this.fileId.getAndIncrement();
	}
}