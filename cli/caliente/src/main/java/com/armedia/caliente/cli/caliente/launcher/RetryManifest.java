package com.armedia.caliente.cli.caliente.launcher;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.importer.ImportRestriction;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectSearchSpec;
import com.armedia.commons.utilities.Tools;

public class RetryManifest {

	private final Logger retriesLog = LoggerFactory.getLogger("retries");

	private final Set<CmfObject.Archetype> types;

	public RetryManifest(Set<CmfObject.Archetype> types) {
		this.types = Tools.freezeCopy(types, true);
	}

	public Set<CmfObject.Archetype> getTypes() {
		return this.types;
	}

	public void logRetry(UUID jobId, CmfObjectSearchSpec object, Throwable thrown) {
		if (!this.types.contains(object.getType())) { return; }
		// We do NOT use CSV formatter b/c we want the *raw* retry ID to be rendered
		// on each line
		this.retriesLog.info(ImportRestriction.render(object));
	}

}