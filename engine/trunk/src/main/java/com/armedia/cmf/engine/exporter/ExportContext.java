package com.armedia.cmf.engine.exporter;

import com.armedia.cmf.engine.TransferContext;

public interface ExportContext<S, V> extends TransferContext<S, V>, ExportListener {
}