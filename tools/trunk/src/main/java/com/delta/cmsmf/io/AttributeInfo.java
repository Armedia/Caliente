package com.delta.cmsmf.io;

import com.delta.cmsmf.io.xml.AttributeT;

public class AttributeInfo {
	private final boolean repeating;
	private final AttributeType type;
	private final int maxLength;
	private final String name;
	private final String id;

	AttributeInfo(boolean repeating, AttributeType type, int maxLength, String name, String id) {
		this.repeating = repeating;
		this.type = type;
		this.maxLength = maxLength;
		this.name = name;
		this.id = id;
	}

	AttributeInfo(AttributeT att) {
		this.repeating = att.isRepeating();
		this.type = att.getType();
		this.maxLength = att.getMaxLength();
		this.name = att.getName();
		this.id = att.getId();
	}

	public boolean isRepeating() {
		return this.repeating;
	}

	public AttributeType getType() {
		return this.type;
	}

	public int getMaxLength() {
		return this.maxLength;
	}

	public String getName() {
		return this.name;
	}

	public String getId() {
		return this.id;
	}

	public AttributeT getXmlVersion() {
		AttributeT att = new AttributeT();
		att.setId(this.id);
		att.setMaxLength(this.maxLength);
		att.setName(this.name);
		att.setRepeating(this.repeating);
		att.setType(this.type);
		return att;
	}
}