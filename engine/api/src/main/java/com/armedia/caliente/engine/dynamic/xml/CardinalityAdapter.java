package com.armedia.caliente.engine.dynamic.xml;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.CheckedCodec;
import com.armedia.commons.utilities.EnumCodec;
import com.armedia.commons.utilities.EnumCodec.Flag;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.xml.AbstractEnumAdapter;

public class CardinalityAdapter extends AbstractEnumAdapter<Cardinality> {

	private static final String STAR = "*";
	private static Set<String> STAR_ALIASES;
	static {
		Set<String> s = new TreeSet<>();
		s.add(CardinalityAdapter.STAR);
		s.add("ALL");
		s.add("ANY");
		CardinalityAdapter.STAR_ALIASES = Tools.freezeSet(new LinkedHashSet<>(s));
	}

	private static final CheckedCodec<Cardinality, String, Exception> STAR_CODEC = new CheckedCodec<Cardinality, String, Exception>() {
		@Override
		public String encode(Cardinality c) throws Exception {
			if (c == Cardinality.ALL) { return CardinalityAdapter.STAR; }
			return null;
		}

		@Override
		public Cardinality decode(String s) throws Exception {
			if (CardinalityAdapter.STAR_ALIASES.contains(StringUtils.upperCase(s))) { return Cardinality.ALL; }
			return null;
		}
	};

	private static final EnumCodec<Cardinality> CODEC = new EnumCodec<>(Cardinality.class,
		CardinalityAdapter.STAR_CODEC, Flag.MARSHAL_FOLDED);

	public CardinalityAdapter() {
		super(CardinalityAdapter.CODEC);
	}
}