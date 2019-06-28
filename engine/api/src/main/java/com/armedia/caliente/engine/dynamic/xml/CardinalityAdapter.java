/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dynamic.xml;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.CheckedCodec;
import com.armedia.commons.utilities.codec.EnumCodec;
import com.armedia.commons.utilities.codec.EnumCodec.Flag;
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