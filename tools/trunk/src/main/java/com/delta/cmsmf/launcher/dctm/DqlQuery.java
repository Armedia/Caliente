package com.delta.cmsmf.launcher.dctm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.armedia.commons.utilities.Tools;

public class DqlQuery {

	public static enum Keyword {
		//
		SELECT,
		FROM,
		ASSEMBLY,
		DOCUMENT,
		FTINDEX,
		IN(ASSEMBLY, DOCUMENT, FTINDEX),
		SEARCH,
		WHERE,
		BY,
		GROUP(BY),
		HAVING,
		UNION,
		ORDER(BY),
		ENABLE,
		//
		;
		private Set<Keyword> chasers;

		private Keyword() {
			this.chasers = Collections.emptySet();
		}

		private Keyword(Keyword... chasers) {
			Set<Keyword> s = new HashSet<Keyword>();
			for (Keyword k : chasers) {
				s.add(k);
			}
			this.chasers = Tools.freezeSet(s);
		}
	}

	public static enum Clause {
		//
		SELECT,
		FROM,
		IN_DOCUMENT,
		IN_ASSEMBLY,
		SEARCH,
		IN_FTINDEX,
		WHERE,
		GROUP_BY,
		HAVING,
		UNION(true),
		ORDER_BY,
		ENABLE,
		//
		;

		private String string;
		private boolean canRepeat;

		private Clause() {
			this(false);
		}

		private Clause(boolean canRepeat) {
			this.string = name().toUpperCase().replace("_", " ");
			String[] s = this.string.split(" ");
			Keyword first = Keyword.valueOf(s[0]);
			if (s.length > 1) {
				Keyword second = Keyword.valueOf(s[1]);
				if (!first.chasers.contains(second)) { throw new RuntimeException(
					String.format("Parser structural error: Keyword [%s] can't be followed by [%s]", first, second)); }
			}
		}

		@Override
		public String toString() {
			return this.string;
		}
	}

	private static Pattern WORD_FINDER = Pattern.compile("\\G(?:\\s*\\b([a-zA-Z]+)\\s+)");

	private static final Map<String, Keyword> KEYWORDS;
	private static final Map<String, Clause> CLAUSES;

	static {
		Map<String, Keyword> kw = new HashMap<String, Keyword>();
		for (Keyword k : Keyword.values()) {
			kw.put(k.name(), k);
		}
		KEYWORDS = Tools.freezeMap(kw);

		Map<String, Clause> cl = new HashMap<String, Clause>();
		for (Clause c : Clause.values()) {
			cl.put(c.string, c);
		}
		CLAUSES = Tools.freezeMap(cl);
	}

	public DqlQuery(String dql) throws Exception {
		this(dql, null);
	}

	private DqlQuery(String dql, AtomicInteger parseCount, Clause... end) throws Exception {

		if (parseCount == null) {
			parseCount = new AtomicInteger(0);
		}
		Set<Clause> endClauses = EnumSet.noneOf(Clause.class);
		if (end != null) {
			for (Clause e : end) {
				endClauses.add(e);
			}
			endClauses = Tools.freezeSet(endClauses);
		}

		// Rules:
		// * Clauses must occurr in the correct order.
		// * Multiple UNION CLAUSES are allowed
		// * UNION DQL doesn't support ORDER_BY or ENABLE
		// * IN DOCUMENT and IN ASSEMBLY are mutually exclusive, but neither is required

		Map<Clause, Object> clauses = new EnumMap<Clause, Object>(Clause.class);

		int startPos = 0;
		Clause lastClause = null;
		int lastStart = 0;
		int lastData = 0;
		Matcher m = DqlQuery.WORD_FINDER.matcher(dql);
		String leading = "";
		while (m.find(startPos)) {
			final String w1 = m.group(1).toUpperCase();
			int clauseStart = m.start(1);
			int dataStart = m.end(1);
			final Keyword kw = DqlQuery.KEYWORDS.get(w1);
			if (kw == null) {
				// Not a keyword
				startPos = m.end(1);
				continue;
			}

			final Clause clause;
			if (kw.chasers.isEmpty()) {
				// Single-word clause
				clause = DqlQuery.CLAUSES.get(w1);
			} else {
				// Find the next word in the matched pattern, and use that
				// to find the clause we're in so we can move the "start"
				// pointer. If the next word isn't one of the expected
				// KEYWORDS, then we ignore this instance and keep going
				if (!m.find()) {
					continue;
				}
				final String w2 = m.group(1).toUpperCase();
				dataStart = m.end(1);
				final Keyword kw2 = DqlQuery.KEYWORDS.get(w2);
				if ((kw2 == null) || !kw.chasers.contains(kw2)) {
					continue;
				}
				clause = DqlQuery.CLAUSES.get(String.format("%s %s", w1, w2));
			}

			if (clause == null) {
				// Not a supported clause, so we ignore it and treat is as data
				startPos = m.end(1);
				continue;
			}

			int nextStartPos = m.end(1);
			if (lastClause != null) {
				Object data = null;
				if (lastClause == Clause.UNION) {
					// Special case: the UNION clause requires special handling - the text inside
					// the union clause must be parsed as its own query until another UNION,
					// ORDER BY or ENABLE clause is found, or the end is reached
					AtomicInteger advance = new AtomicInteger(0);
					List<DqlQuery> unions = null;
					data = clauses.get(Clause.UNION);
					if (data == null) {
						unions = new ArrayList<DqlQuery>();
						clauses.put(Clause.UNION, unions);
					} else {
						@SuppressWarnings("unchecked")
						List<DqlQuery> u = (List<DqlQuery>) data;
						unions = u;
					}
					unions.add(
						new DqlQuery(dql.substring(lastData), advance, Clause.UNION, Clause.ORDER_BY, Clause.ENABLE));
					nextStartPos = startPos + advance.get();
				} else {
					// Validate that this clause is occurring in the correct order with
					// respect to the others
					if (clause.ordinal() < lastClause.ordinal()) { throw new Exception(
						String.format("The %s clause must be declared before the %s clause (at index %d)",
							clause.string, lastClause.string, clauseStart)); }
					// Validate that this clause allows repetition
					if ((clause == lastClause) && !clause.canRepeat) { throw new Exception(
						String.format("The %s clause can't be repeated (at index %d)", clause.string, clauseStart)); }

					// We have a previous clause, so we must also have its previous start
					// position, which means we can parse out its crap
					data = dql.substring(lastData, clauseStart).trim();
				}
				clauses.put(lastClause, data);
			} else {
				leading = dql.substring(0, clauseStart);
			}

			// Advance the parser position
			startPos = nextStartPos;
			if (endClauses.contains(clause)) {
				// we're done
				startPos = clauseStart;
				break;
			}

			// Now we have the clause. We make note of its start position
			lastClause = clause;
			lastStart = clauseStart;
			lastData = dataStart;
		}
		parseCount.set(startPos);
	}
}
