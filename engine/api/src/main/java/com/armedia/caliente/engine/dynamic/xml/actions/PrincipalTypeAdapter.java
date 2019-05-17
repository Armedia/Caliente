package com.armedia.caliente.engine.dynamic.xml.actions;

import com.armedia.caliente.engine.PrincipalType;
import com.armedia.commons.utilities.xml.EnumCodec;

public class PrincipalTypeAdapter extends EnumCodec<PrincipalType> {
	public PrincipalTypeAdapter() {
		super(PrincipalType.class);
	}
}