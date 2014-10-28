package com.armedia.cmf.importer;

import com.armedia.cmf.engine.TransferContext;

public interface ImportContext<S, V> extends TransferContext<S, V>, ImportListener {
}