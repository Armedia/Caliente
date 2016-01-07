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

class DqlQuery {

	static class ClauseGenerator {
		public String generate(int nestLevel, Clause clause, String data) {
			if (clause == null) { return data; }
			return String.format("%s %s", clause, data);
		}

		public String generate(int nestLevel, Clause clause, DqlQuery nestedQuery) {
			if (clause == null) { return nestedQuery.toString(this, nestLevel); }
			return String.format("%s %s", clause, nestedQuery.toString(this, nestLevel));
		}
	}

	static enum Keyword {
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

	static enum Clause {
		//
		SELECT, FROM, IN_DOCUMENT, IN_ASSEMBLY, SEARCH, IN_FTINDEX, WHERE, GROUP_BY, HAVING, UNION, ORDER_BY, ENABLE,
		//
		;

		private String string;

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

	private static class ClausePosition {
		private final Clause clause;
		private final int clauseStart;
		private final int dataStart;

		private ClausePosition(Clause clause, int clauseStart, int dataStart) {
			this.clause = clause;
			this.clauseStart = clauseStart;
			this.dataStart = dataStart;
		}
	}

	private static class Parser {
		private final Matcher m;
		private int startPos = 0;

		public Parser(String dql) {
			this.m = DqlQuery.WORD_FINDER.matcher(dql);
			this.startPos = 0;
		}

		/**
		 * <p>
		 * Will find the next {@link ClausePosition} in the string, or {@code null} if there are no
		 * more.
		 * </p>
		 *
		 * @return the next {@link ClausePosition} in the string, or {@code null} if there are no
		 *         more.
		 */
		public ClausePosition findNextClause() throws Exception {
			while (this.m.find(this.startPos)) {
				final String w1 = this.m.group(1).toUpperCase();
				int clauseStart = this.m.start(1);
				int dataStart = this.m.end(1);
				final Keyword kw = DqlQuery.KEYWORDS.get(w1);
				if (kw == null) {
					// Not a keyword
					this.startPos = this.m.end(1);
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
					if (!this.m.find()) {
						continue;
					}
					final String w2 = this.m.group(1).toUpperCase();
					dataStart = this.m.end(1);
					final Keyword kw2 = DqlQuery.KEYWORDS.get(w2);
					if ((kw2 == null) || !kw.chasers.contains(kw2)) {
						// This next keyword isn't one of the required chasers, and thus
						// constitutes a syntax error
						throw new Exception(String.format("The keyword %s (at index %d) must be followed by one of %s",
							kw, this.m.start(1), kw.chasers));
					}
					clause = DqlQuery.CLAUSES.get(String.format("%s %s", w1, w2));
				}

				this.startPos = this.m.end(1);
				if (clause != null) { return new ClausePosition(clause, clauseStart, dataStart); }
			}
			this.startPos = this.m.regionEnd();
			return null;
		}
	}

	private final String leading;
	private final Map<Clause, Object> clauses;

	DqlQuery(String dql) throws Exception {
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

		String leading = "";
		ClausePosition previousClause = null;
		Parser p = new Parser(dql);
		ClausePosition clause = p.findNextClause();
		while (clause != null) {
			if (endClauses.contains(clause.clause)) {
				parseCount.set(clause.clauseStart);
				break;
			}
			if (previousClause == null) {
				leading = dql.substring(0, clause.clauseStart).trim();
			}

			ClausePosition nextClause = p.findNextClause();

			Object data = null;
			if (clause.clause == Clause.UNION) {
				// This is a UNION clause, parse everything else
				List<DqlQuery> unions = null;
				data = clauses.get(clause.clause);
				if (data == null) {
					unions = new ArrayList<DqlQuery>();
					data = unions;
				} else {
					@SuppressWarnings("unchecked")
					List<DqlQuery> u = (List<DqlQuery>) data;
					unions = u;
				}
				AtomicInteger advance = new AtomicInteger(0);
				unions.add(new DqlQuery(dql.substring(clause.dataStart), advance, Clause.UNION, Clause.ORDER_BY,
					Clause.ENABLE));
				// "advance" contains the number of bytes the parser must be moved forward
				p.startPos = clause.dataStart + advance.get();
				nextClause = p.findNextClause();
			} else if (nextClause == null) {
				// The remaining text is the data for this clause
				data = dql.substring(clause.dataStart).trim();
			} else {
				if (nextClause.clause.ordinal() < clause.clause.ordinal()) { throw new Exception(
					String.format("SQL Clauses out of order: %s (at index %d) can't come before %s (at index %d)",
						clause.clause, clause.clauseStart, nextClause.clause, nextClause.clauseStart)); }
				data = dql.substring(clause.dataStart, nextClause.clauseStart).trim();
			}
			clauses.put(clause.clause, data);
			previousClause = clause;
			clause = nextClause;
		}

		if (previousClause == null) {
			leading = dql.substring(0, p.startPos);
		}
		this.leading = leading;
		this.clauses = clauses;
	}

	@Override
	public String toString() {
		return toString(null);
	}

	public String toString(ClauseGenerator generator) {
		return toString(generator, 0);
	}

	private String toString(ClauseGenerator generator, int nestLevel) {
		++nestLevel;
		if (generator == null) {
			generator = new ClauseGenerator();
		}
		StringBuilder b = new StringBuilder();
		b.append(generator.generate(nestLevel, null, this.leading));
		for (Clause c : this.clauses.keySet()) {
			b.append(" ");
			Object data = this.clauses.get(c);
			if (data instanceof List) {
				List<?> l = List.class.cast(data);
				boolean first = true;
				for (Object o : l) {
					if (first) {
						first = false;
					} else {
						b.append(" ");
					}
					String s = null;
					if (o instanceof DqlQuery) {
						s = generator.generate(nestLevel, c, DqlQuery.class.cast(o));
					} else {
						s = generator.generate(nestLevel, c, o.toString());
					}
					b.append(s);
				}
			} else {
				b.append(generator.generate(nestLevel, c, data.toString()));
			}
		}
		return b.toString().trim();
	}
}