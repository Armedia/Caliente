package com.armedia.cmf.engine.tools;

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.commons.utilities.FileNameTools;

public abstract class AclTools {

	private static final char ACTION_SEP = '|';

	public static String encodeActions(Set<String> actions) {
		if ((actions == null) || actions.isEmpty()) { return ""; }
		return FileNameTools.reconstitute(actions, false, false, AclTools.ACTION_SEP);
	}

	public static Set<String> decodeActions(String str) {
		if (str == null) { return new TreeSet<String>(); }
		return new TreeSet<String>(new StrTokenizer(str, AclTools.ACTION_SEP).getTokenList());
	}

	private AclTools() {
	}
}
