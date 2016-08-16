package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;

public class AlfUserDelegate extends AlfImportDelegate {

	public AlfUserDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException {
		CmfValue group = getAttributeValue("dctm:r_is_group");
		if ((group != null) && group.asBoolean()) { return Collections.singleton(ImportOutcome.SKIPPED); }
		CmfValue name = getAttributeValue("cmis:name");
		if (name == null) { return Collections.singleton(ImportOutcome.SKIPPED); }
		CmfValue login = getAttributeValue("cmf:login_name");
		if (login == null) { return Collections.singleton(ImportOutcome.SKIPPED); }

		ctx.printf("Mapping username [%s] to loginname [%s]...", name.asString(), login.asString());
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, login.asString(), login.asString()));
	}
}