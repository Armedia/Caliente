package com.delta.cmsmf.utils;

import com.documentum.fc.client.IDfDocument;

public class DfDocumentTreePatch extends DfVersionTreePatch<IDfDocument> implements IDfDocument {

	public DfDocumentTreePatch(IDfDocument base, DfVersionNumber patchNumber) {
		super(IDfDocument.class, base, patchNumber);
	}

}