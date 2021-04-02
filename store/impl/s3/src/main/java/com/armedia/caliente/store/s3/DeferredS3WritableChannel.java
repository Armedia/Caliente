package com.armedia.caliente.store.s3;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import com.armedia.caliente.store.tools.DeferredWritableChannel;
import com.armedia.commons.utilities.Tools;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

public class DeferredS3WritableChannel extends DeferredWritableChannel {

	private static Path createTempFile(Path tempDir, URI uri) throws IOException {
		if (tempDir == null) {
			// Get the system's default temp directory...
			tempDir = Paths.get(System.getProperty("java.io.tmpdir")).normalize().toRealPath();
		}
		String bucket = Objects.requireNonNull(uri, "Must provide a content URI").getHost();
		Path bucketDir = Objects.requireNonNull(tempDir, "Must provide a temporary directory").resolve(bucket);
		return Files.createTempFile(bucketDir, null, null);
	}

	private final S3Client client;
	private final String bucket;
	private final String key;
	private final Map<String, String> metadata;

	public DeferredS3WritableChannel(Path tempDir, S3Client client, URI uri) throws IOException {
		this(tempDir, client, uri, null);
	}

	public DeferredS3WritableChannel(Path tempDir, S3Client client, URI uri, Map<String, String> metadata)
		throws IOException {
		super(DeferredS3WritableChannel.createTempFile(tempDir, uri));
		this.client = Objects.requireNonNull(client, "Must provide an S3Client instance");
		this.bucket = Objects.requireNonNull(uri, "Must provide a URI to upload to").getHost();
		this.key = uri.getPath();
		this.metadata = Tools.freezeCopy(metadata);
	}

	@Override
	protected void process(ReadableByteChannel in, long size) throws IOException {
		this.client.putObject((R) -> {
			R.bucket(this.bucket);
			R.key(this.key);
			if ((this.metadata != null) && !this.metadata.isEmpty()) {
				R.metadata(this.metadata);
			}
		}, RequestBody.fromInputStream(Channels.newInputStream(in), size));
	}
}