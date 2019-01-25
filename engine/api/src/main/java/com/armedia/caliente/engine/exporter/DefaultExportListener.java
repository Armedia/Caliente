package com.armedia.caliente.engine.exporter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectSearchSpec;

public class DefaultExportListener implements ExportListener {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void objectExportStarted(UUID jobId, CmfObjectSearchSpec object, CmfObjectSearchSpec referrent) {
	}

	@Override
	public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber) {
	}

	@Override
	public void objectSkipped(UUID jobId, CmfObjectSearchSpec object, ExportSkipReason reason, String extraInfo) {
	}

	@Override
	public void objectExportFailed(UUID jobId, CmfObjectSearchSpec object, Throwable thrown) {
	}

	@Override
	public void consistencyWarning(UUID jobId, CmfObjectSearchSpec object, String fmt, Object... args) {
	}
}