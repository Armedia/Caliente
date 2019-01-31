package com.armedia.caliente.engine.dfc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.documentum.fc.client.IDfVirtualDocumentNode;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public class DctmVdocMember {

	// [childId|chronId|binding|followAssembly|lateBinding]{name}
	private static final String ENCODING_FORMAT = "[%s|%s|%s|%s|%s]{%s}";
	private static final Pattern PARSER = Pattern.compile("^\\[([^{]*)\\]\\{(.*)\\}$");

	private final IDfId childId;
	private final IDfId chronicleId;
	private final String binding;
	private final boolean followAssembly;
	private final boolean overrideLateBinding;
	private final String name;
	private final String encoded;

	public DctmVdocMember(IDfVirtualDocumentNode node) throws DfException {
		this.childId = node.getId();
		this.chronicleId = node.getChronId();
		this.binding = node.getBinding();
		this.followAssembly = node.getFollowAssembly();
		this.overrideLateBinding = node.getOverrideLateBindingValue();
		this.name = node.getSelectedObject().getObjectName();
		this.encoded = String.format(DctmVdocMember.ENCODING_FORMAT, this.childId.getId(), this.chronicleId.getId(),
			this.binding, this.followAssembly, this.overrideLateBinding, this.name);
	}

	public DctmVdocMember(String encoded) {
		if (StringUtils.isEmpty(encoded)) {
			throw new IllegalArgumentException(
				String.format("The given string [%s] is not a valid virtual document member record", encoded));
		}
		Matcher m = DctmVdocMember.PARSER.matcher(encoded);
		if (!m.matches()) {
			throw new IllegalArgumentException(
				String.format("The given string [%s] is not a valid virtual document member record", encoded));
		}

		this.name = m.group(2);
		String[] s = m.group(1).split("\\|");
		this.childId = new DfId(s[0]);
		this.chronicleId = new DfId(s[1]);
		this.binding = s[2];
		this.followAssembly = Boolean.valueOf(s[3]);
		this.overrideLateBinding = Boolean.valueOf(s[4]);
		this.encoded = encoded;
	}

	public IDfId getChildId() {
		return this.childId;
	}

	public IDfId getChronicleId() {
		return this.chronicleId;
	}

	public String getBinding() {
		return this.binding;
	}

	public String getName() {
		return this.name;
	}

	public boolean isFollowAssembly() {
		return this.followAssembly;
	}

	public boolean isOverrideLateBinding() {
		return this.overrideLateBinding;
	}

	public String getEncoded() {
		return this.encoded;
	}

	@Override
	public String toString() {
		return String.format(
			"DctmVdocMember [childId=%s, chronicleId=%s, binding=%s, followAssembly=%s, overrideLateBinding=%s, name=%s]",
			this.childId, this.chronicleId, this.binding, this.followAssembly, this.overrideLateBinding, this.name);
	}
}