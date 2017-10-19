package com.armedia.caliente.engine.alfresco.bi.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class AlfImportUserDelegate extends AlfImportDelegate {

	public AlfImportUserDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException {

		// Special dispensation for Documentum...
		CmfValue group = getAttributeValue("dctm:r_is_group");
		if ((group != null) && group.asBoolean()) { return Collections.singleton(ImportOutcome.SKIPPED); }

		CmfValue name = getAttributeValue("cmis:name");
		if (name == null) { return Collections.singleton(ImportOutcome.SKIPPED); }
		CmfValue login = getAttributeValue("cmf:login_name");
		if (login == null) { return Collections.singleton(ImportOutcome.SKIPPED); }

		if (this.factory.mapUserLogin(name.asString(), login.asString())) {
			ctx.printf("Mapped username [%s] to loginname [%s]...", name.asString(), login.asString());
		}
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, login.asString(), login.asString()));
	}
}