package com.armedia.caliente.engine.dynamic.xml;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class CardinalityAdapter extends XmlAdapter<String, Cardinality> {

	private static final String STAR = "*";
	private static Set<String> STAR_ALIASES;
	static {
		Set<String> s = new TreeSet<>();
		s.add(CardinalityAdapter.STAR);
		s.add("ALL");
		s.add("ANY");
		CardinalityAdapter.STAR_ALIASES = Tools.freezeSet(new LinkedHashSet<>(s));
	}

	@Override
	public Cardinality unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v);
		v = StringUtils.upperCase(v);
		if (CardinalityAdapter.STAR_ALIASES.contains(v)) { return Cardinality.ALL; }
		return Cardinality.valueOf(v.toUpperCase());
	}

	@Override
	public String marshal(Cardinality v) throws Exception {
		if (v == null) { return null; }
		switch (v) {
			case ALL:
				return CardinalityAdapter.STAR;
			default:
				return v.name().toLowerCase();
		}
	}

}