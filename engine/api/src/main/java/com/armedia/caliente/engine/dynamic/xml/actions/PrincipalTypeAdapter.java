package com.armedia.caliente.engine.dynamic.xml.actions;

import com.armedia.caliente.engine.PrincipalType;
import com.armedia.commons.utilities.XmlEnumAdapter;

public class PrincipalTypeAdapter extends XmlEnumAdapter<PrincipalType> {
	public PrincipalTypeAdapter() {
		super(PrincipalType.class);
	}
}