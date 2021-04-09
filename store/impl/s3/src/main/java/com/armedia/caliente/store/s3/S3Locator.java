package com.armedia.caliente.store.s3;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedConsumer;

final class S3Locator implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String bucket;
	private final String key;
	private final String versionId;
	private final URI uri;

	S3Locator(String encoded) throws URISyntaxException {
		// Parse it out!
		this.uri = new URI(encoded);
		if (!Objects.equals(this.uri.getScheme(), S3ContentStoreFactory.URI_SCHEME)) {
			throw new IllegalArgumentException("The scheme [" + this.uri.getScheme() + "] is not supported for S3");
		}
		this.bucket = this.uri.getHost();
		this.key = this.uri.getPath().replaceAll("/+", "/").replaceAll("^/", StringUtils.EMPTY);
		this.versionId = Tools.coalesce(this.uri.getFragment(), StringUtils.EMPTY);
	}

	S3Locator(String bucket, String key) {
		this(bucket, key, null);
	}

	S3Locator(String bucket, String key, String versionId) {
		this.bucket = bucket;
		if (StringUtils.isBlank(bucket)) { throw new IllegalArgumentException("Must provide a bucket name"); }
		this.key = key;
		if (StringUtils.isBlank(key)) { throw new IllegalArgumentException("Must provide an object key"); }
		this.versionId = Tools.coalesce(versionId, StringUtils.EMPTY);
		try {
			this.uri = new URI(S3ContentStoreFactory.URI_SCHEME, bucket, "/" + key, versionId);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to create a URI for an S3Locator", e);
		}
	}

	public <T extends Exception> S3Locator bucket(CheckedConsumer<String, T> c) throws T {
		if ((c != null) && StringUtils.isNotBlank(this.bucket)) {
			c.acceptChecked(this.bucket);
		}
		return this;
	}

	public String bucket() {
		return this.bucket;
	}

	public <T extends Exception> S3Locator key(CheckedConsumer<String, T> c) throws T {
		if ((c != null) && StringUtils.isNotBlank(this.key)) {
			c.acceptChecked(this.key);
		}
		return this;
	}

	public String key() {
		return this.key;
	}

	public <T extends Exception> S3Locator versionId(CheckedConsumer<String, T> c) throws T {
		if ((c != null) && StringUtils.isNotBlank(this.versionId)) {
			c.acceptChecked(this.versionId);
		}
		return this;
	}

	public String versionId() {
		return this.versionId;
	}

	public URI uri() {
		return this.uri;
	}

	@Override
	public String toString() {
		return this.uri.toASCIIString();
	}
}