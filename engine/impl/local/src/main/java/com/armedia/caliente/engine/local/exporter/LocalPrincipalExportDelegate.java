package com.armedia.caliente.engine.local.exporter;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfValueType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfArchetype;
import com.armedia.caliente.store.CmfValue;

public class LocalPrincipalExportDelegate extends LocalExportDelegate<Principal> {

	protected LocalPrincipalExportDelegate(LocalExportDelegateFactory factory, LocalRoot root, Principal object)
		throws Exception {
		super(factory, root, Principal.class, object);
	}

	@Override
	protected Collection<LocalPrincipalExportDelegate> identifyRequirements(CmfObject<CmfValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected int calculateDependencyTier(LocalRoot root, Principal p) throws Exception {
		return 0;
	}

	@Override
	protected boolean marshal(LocalExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		CmfAttribute<CmfValue> att = null;
		att = new CmfAttribute<>(IntermediateAttribute.NAME, CmfValueType.STRING, false);
		att.setValue(new CmfValue(this.object.getName()));
		object.setAttribute(att);
		if (object.getType() == CmfArchetype.USER) {
			att = new CmfAttribute<>(IntermediateAttribute.LOGIN_NAME, CmfValueType.STRING, false);
			att.setValue(new CmfValue(this.object.getName()));
			object.setAttribute(att);
		}
		return true;
	}

	@Override
	protected Collection<LocalPrincipalExportDelegate> identifyDependents(CmfObject<CmfValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected CmfArchetype calculateType(LocalRoot root, Principal p) throws Exception {
		if (GroupPrincipal.class.isInstance(p)) { return CmfArchetype.GROUP; }
		if (UserPrincipal.class.isInstance(p)) { return CmfArchetype.USER; }
		throw new ExportException(String.format("Principal object [%s] is of an unknown type or doesn't exist", p));
	}

	@Override
	protected String calculateLabel(LocalRoot root, Principal object) throws Exception {
		return object.getName();
	}

	@Override
	protected String calculateObjectId(LocalRoot root, Principal object) throws Exception {
		return DigestUtils.sha256Hex(String.format("%s:%s", object.getClass().getCanonicalName(), object.getName()));
	}

	@Override
	protected String calculateSearchKey(LocalRoot root, Principal object) throws Exception {
		return object.getName();
	}

	@Override
	protected String calculateName(LocalRoot root, Principal object) throws Exception {
		return object.getName();
	}

	@Override
	protected boolean calculateHistoryCurrent(LocalRoot root, Principal object) throws Exception {
		// Always true
		return true;
	}

	@Override
	protected List<CmfContentStream> storeContent(LocalExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		return null;
	}
}