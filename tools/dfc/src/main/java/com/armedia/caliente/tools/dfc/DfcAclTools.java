package com.armedia.caliente.tools.dfc;

import org.apache.commons.lang3.StringUtils;

import com.documentum.fc.client.IDfACL;

public class DfcAclTools {

	private DfcAclTools() {
	}

	public static boolean isValidPermit(int permit) {
		switch (permit) {
			case IDfACL.DF_PERMIT_BROWSE:
			case IDfACL.DF_PERMIT_DELETE:
			case IDfACL.DF_PERMIT_NONE:
			case IDfACL.DF_PERMIT_READ:
			case IDfACL.DF_PERMIT_RELATE:
			case IDfACL.DF_PERMIT_VERSION:
			case IDfACL.DF_PERMIT_WRITE:
				return true;
			default:
				return false;
		}
	}

	public static boolean isValidPermit(String permit) {
		if (StringUtils.isEmpty(permit)) { return false; }
		switch (permit) {
			case IDfACL.DF_PERMIT_BROWSE_STR:
			case IDfACL.DF_PERMIT_DELETE_STR:
			case IDfACL.DF_PERMIT_NONE_STR:
			case IDfACL.DF_PERMIT_READ_STR:
			case IDfACL.DF_PERMIT_RELATE_STR:
			case IDfACL.DF_PERMIT_VERSION_STR:
			case IDfACL.DF_PERMIT_WRITE_STR:
				return true;
			default:
				return false;
		}
	}
}