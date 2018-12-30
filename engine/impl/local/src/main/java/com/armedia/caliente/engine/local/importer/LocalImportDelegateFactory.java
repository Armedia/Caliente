package com.armedia.caliente.engine.local.importer;

import java.io.IOException;

import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.importer.schema.decl.SchemaDeclarationServiceException;
import com.armedia.caliente.engine.importer.schema.decl.SchemaService;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportDelegateFactory
	extends ImportDelegateFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalImportContext, LocalImportEngine> {

	private final boolean includeAllVersions;
	private final boolean failOnCollisions;

	public LocalImportDelegateFactory(LocalImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		this.includeAllVersions = configuration.getBoolean(LocalSetting.INCLUDE_ALL_VERSIONS);
		this.failOnCollisions = configuration.getBoolean(LocalSetting.FAIL_ON_COLLISIONS);
	}

	public final boolean isIncludeAllVersions() {
		return this.includeAllVersions;
	}

	public final boolean isFailOnCollisions() {
		return this.failOnCollisions;
	}

	@Override
	protected LocalImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case DOCUMENT:
				return new LocalDocumentImportDelegate(this, storedObject);
			case FOLDER:
				return new LocalFolderImportDelegate(this, storedObject);
			default:
				return null;
		}
	}

	@Override
	protected SchemaService newSchemaService(LocalRoot session) throws SchemaDeclarationServiceException {
		return null;
	}
}