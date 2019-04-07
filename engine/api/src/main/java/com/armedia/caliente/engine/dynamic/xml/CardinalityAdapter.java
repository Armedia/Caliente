package com.armedia.caliente.engine.dynamic.xml;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlEnumAdapter;

public class CardinalityAdapter extends XmlEnumAdapter<Cardinality> {

	private static final String STAR = "*";
	private static Set<String> STAR_ALIASES;
	static {
		Set<String> s = new TreeSet<>();
		s.add(CardinalityAdapter.STAR);
		s.add("ALL");
		s.add("ANY");
		CardinalityAdapter.STAR_ALIASES = Tools.freezeSet(new LinkedHashSet<>(s));
	}

	public CardinalityAdapter() {
		super(Cardinality.class, Flag.MARSHAL_FOLDED);
	}

	@Override
	protected Cardinality specialUnmarshal(String v) throws Exception {
		if (CardinalityAdapter.STAR_ALIASES.contains(StringUtils.upperCase(v))) { return Cardinality.ALL; }
		return null;
	}

	@Override
	protected String specialMarshal(Cardinality e) throws Exception {
		if (e == Cardinality.ALL) { return CardinalityAdapter.STAR; }
		return null;
	}
}