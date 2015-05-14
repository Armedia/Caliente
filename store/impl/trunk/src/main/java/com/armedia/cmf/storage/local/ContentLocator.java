package com.armedia.cmf.storage.local;

import java.net.URI;

import com.armedia.cmf.storage.StoredObject;

class ContentLocator {

	final URI uri;
	final StoredObject<?> storedObject;

	ContentLocator(StoredObject<?> storedObject, URI uri) {
		this.uri = uri;
		this.storedObject = storedObject;
	}

}