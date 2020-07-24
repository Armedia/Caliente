package com.armedia.caliente.engine.converter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.armedia.commons.utilities.CollectionTools;
import com.armedia.commons.utilities.Tools;

public class PathIdHelper {

	private static final char SEP = '/';

	private static final Function<String, String> ENCODER = Tools.getSeparatorEscaper(PathIdHelper.SEP);
	private static final Function<String, String> DECODER = Tools.getSeparatorUnescaper(PathIdHelper.SEP);

	public static String encode(String id) {
		return PathIdHelper.ENCODER.apply(id);
	}

	public static Iterator<String> encode(final Iterator<String> it) {
		return CollectionTools.map(PathIdHelper.ENCODER, it);
	}

	public static Iterable<String> encode(Iterable<String> it) {
		return CollectionTools.map(PathIdHelper.ENCODER, it);
	}

	public static String encodePaths(Collection<String> pathIds) {
		return Tools.joinEscaped(PathIdHelper.SEP, pathIds);
	}

	public static String decode(String id) {
		return PathIdHelper.DECODER.apply(id);
	}

	public static Iterator<String> decode(final Iterator<String> it) {
		return CollectionTools.map(PathIdHelper.DECODER, it);
	}

	public static Iterable<String> decode(Iterable<String> it) {
		return CollectionTools.map(PathIdHelper.DECODER, it);
	}

	public static List<String> decodePaths(String path) {
		return Tools.splitEscaped(PathIdHelper.SEP, path);
	}
}