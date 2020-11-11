package com.armedia.caliente.engine.local.exporter;

import java.util.LinkedHashMap;
import java.util.Map;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.Codec;
import com.armedia.commons.utilities.codec.FunctionalCodec;

public enum LocalSearchType {

	//
	DIRECTORY("directory"), // Recursive directory search, like before
	LIST_FILE("list"), // A file containing paths within
	SQL(), // A SQL Query
	//
	;

	private final String tag;

	private LocalSearchType(String tag) {
		this.tag = Tools.coalesce(tag, name());
	}

	private LocalSearchType() {
		this(null);
	}

	public String getTag() {
		return this.tag;
	}

	private static final Map<String, LocalSearchType> DECODER;
	public static final Codec<LocalSearchType, String> CODEC;

	static {
		Map<String, LocalSearchType> d = new LinkedHashMap<>();
		for (LocalSearchType t : LocalSearchType.values()) {
			LocalSearchType o = d.put(t.tag, t);
			if (o != null) {
				throw new RuntimeException("LocalSearchType values " + o.name() + " and " + t.name()
					+ " can't both handle the tag <" + t.tag + ">");
			}
		}
		DECODER = Tools.freezeMap(d);

		FunctionalCodec.Builder<LocalSearchType, String> b = new FunctionalCodec.Builder<>();
		b.setEncoder(LocalSearchType::getTag);
		b.setDecoder(LocalSearchType.DECODER::get);
		CODEC = b.build();
	}

}