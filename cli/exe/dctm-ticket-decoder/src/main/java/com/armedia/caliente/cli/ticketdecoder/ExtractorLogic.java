/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.cli.ticketdecoder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Page;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedPredicate;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class ExtractorLogic implements PooledWorkersLogic<IDfSession, IDfId, Exception> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static final Comparator<Rendition> RENDITION_BY_DATE_ASC = (a, b) -> Tools.compare(a.getDate(),
		b.getDate());
	private static final Comparator<Rendition> RENDITION_BY_MODIFIER_ASC = (a, b) -> Tools.compare(a.getModifier(),
		b.getModifier());

	// Documentum trims spaces so let's use that "feature"
	private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();
	private static final Pattern SIMPLE_PRIORITY_PARSER = Pattern
		.compile("^(?:([0-3]{1,4}):)?(?:([*]|[^*:@%]+)(?:%(.*?))?)?(?:@(old|new)(?:est)?)?$", Pattern.CASE_INSENSITIVE);
	private static final String TYPE_CANDIDATES = "0123";
	private static final String FORMAT_WILDCARD = "*";
	private static final BiPredicate<Rendition, RenditionIndexGroup> NON_NULL = (r, m) -> (r != null);

	private static final String LEAST = "-";
	private static final String MOST = "+";
	private static final String OLDEST = "old";
	private static final String NEWEST = "new";

	private enum RenditionIndexType {
		//
		DATE(ExtractorLogic.RENDITION_BY_DATE_ASC), //
		MODIFIER(ExtractorLogic.RENDITION_BY_MODIFIER_ASC), //
		//
		;

		private final Comparator<Rendition> comparator;

		private RenditionIndexType(Comparator<Rendition> comparator) {
			this.comparator = Objects.requireNonNull(comparator,
				String.format("The value %s has a null comparator", name()));
		}
	}

	private class RenditionIndex {
		private final RenditionIndexType type;
		private final Map<String, SortedSet<Rendition>> index = new HashMap<>();
		private final SortedSet<Rendition> all;

		public RenditionIndex(RenditionIndexType type) {
			this.type = Objects.requireNonNull(type);
			this.all = new TreeSet<>(this.type.comparator);
		}

		public void add(Rendition r) {
			if (r == null) { return; }
			SortedSet<Rendition> set = this.index.get(r.getFormat());
			if (set == null) {
				set = new TreeSet<>(this.type.comparator);
				this.index.put(r.getFormat(), set);
			}
			set.add(r);
			this.all.add(r);
		}

		public SortedSet<Rendition> get(String format) {
			if ((format == null) || ExtractorLogic.FORMAT_WILDCARD.equals(format)) { return this.all; }
			if (!this.index.containsKey(format)) { return Collections.emptySortedSet(); }
			return this.index.get(format);
		}
	}

	private class RenditionIndexGroup {
		private final Map<RenditionIndexType, RenditionIndex> indexes;

		private RenditionIndexGroup(Iterable<Rendition> renditions, RenditionIndexType... types) {
			this(renditions, (types != null) && (types.length > 0) ? Arrays.asList(types) : null);
		}

		private RenditionIndexGroup(Iterable<Rendition> renditions, Iterable<RenditionIndexType> types) {
			Map<RenditionIndexType, RenditionIndex> indexes = new EnumMap<>(RenditionIndexType.class);
			if ((types != null) && (renditions != null)) {
				Consumer<Rendition> c = null;
				for (RenditionIndexType type : types) {
					if (type == null) {
						continue;
					}
					RenditionIndex idx = new RenditionIndex(type);
					indexes.put(type, idx);
					if (c != null) {
						c = c.andThen(idx::add);
					} else {
						c = idx::add;
					}
				}
				renditions.forEach(c);
			}
			this.indexes = Tools.freezeMap(indexes);
		}

		public SortedSet<Rendition> get(RenditionIndexType type, String format) {
			if ((type == null) || !this.indexes.containsKey(type)) { return Collections.emptySortedSet(); }
			return this.indexes.get(type).get(format);
		}
	}

	private static boolean isNaturalNumber(Number n) {
		if (Byte.class.isInstance(n)) { return true; }
		if (Short.class.isInstance(n)) { return true; }
		if (Integer.class.isInstance(n)) { return true; }
		if (Long.class.isInstance(n)) { return true; }
		if (BigInteger.class.isInstance(n)) { return true; }
		return false;
	}

	private static boolean coerceBooleanResult(Object o) {
		// If it's a null, then it's a false right away
		if (o == null) { return false; }

		// Is it a boolean? Sweet!
		if (Boolean.class.isInstance(o)) { return Boolean.class.cast(o); }

		// Is it a number? 0 == false, non-0 == true
		if (Number.class.isInstance(o)) {
			Number n = Number.class.cast(o);
			if (ExtractorLogic.isNaturalNumber(n)) {
				if (BigInteger.class.isInstance(n)) {
					return !BigInteger.class.cast(n).equals(BigInteger.ZERO);
				} else {
					return (n.longValue() == 0);
				}
			} else {
				if (BigDecimal.class.isInstance(n)) {
					return !BigDecimal.class.cast(n).equals(BigDecimal.ZERO);
				} else {
					double v = n.doubleValue();
					return (Double.max(v, 0.0) != Double.min(v, 0.0));
				}
			}
		}

		// Neither a boolean nor a number...must be a string...
		String str = Tools.toString(o);
		if (StringUtils.equalsIgnoreCase(Boolean.FALSE.toString(), str)) { return false; }
		if (StringUtils.equalsIgnoreCase(Boolean.TRUE.toString(), str)) { return true; }

		// Not-null == true...
		return true;
	}

	static <T> CheckedPredicate<T, ScriptException> compileFilter(Class<T> klazz, final String expression)
		throws ScriptException {
		CheckedPredicate<T, ScriptException> p = klazz::isInstance;
		if (expression != null) {
			// Compile the script
			ScriptEngine engine = ExtractorLogic.ENGINE_MANAGER.getEngineByName("jexl");
			if (engine != null) {
				CheckedPredicate<T, ScriptException> scriptPredicate = null;
				final String varName = klazz.getSimpleName().toLowerCase();
				if (Compilable.class.isInstance(engine)) {
					// Compile, for speed
					Compilable compiler = Compilable.class.cast(engine);
					CompiledScript script = compiler.compile(expression);
					scriptPredicate = (obj) -> {
						final Bindings b = new SimpleBindings();
						b.put(varName, obj);
						return ExtractorLogic.coerceBooleanResult(script.eval(b));
					};
				} else {
					scriptPredicate = (obj) -> {
						final Bindings b = new SimpleBindings();
						b.put(varName, obj);
						return ExtractorLogic.coerceBooleanResult(engine.eval(expression, b));
					};
				}
				p = p.and(scriptPredicate);
			}
		}
		return p;
	}

	private static BiPredicate<Rendition, RenditionIndexGroup> compilePrioritizer(String priority) {
		Matcher m = ExtractorLogic.SIMPLE_PRIORITY_PARSER.matcher(priority);
		if (!m.matches()) { return null; }
		BiPredicate<Rendition, RenditionIndexGroup> p = ExtractorLogic.NON_NULL;
		final String type = m.group(1);
		final String format = m.group(2);
		final String modifier = m.group(3);
		final String age = m.group(4);

		// If a rendition type is specified, add it
		if (type != null) {
			final BitSet bs = new BitSet(ExtractorLogic.TYPE_CANDIDATES.length());
			bs.clear();
			for (int i = 0; i < type.length(); i++) {
				try {
					bs.set(Integer.parseInt(type.substring(i, i + 1)));
				} catch (NumberFormatException e) {
					continue;
				}
			}
			p = p.and((rendition, peers) -> bs.get(rendition.getType()));
		}

		// Add the format
		if (format != null) {
			// If we're not using a format wildcard...
			if (!StringUtils.equals(ExtractorLogic.FORMAT_WILDCARD, format)) {
				p = p.and((rendition, peers) -> Tools.equals(rendition.getFormat(), format));
			}
			// If a modifier is specified, add it
			if (modifier != null) {
				if (ExtractorLogic.LEAST.equals(modifier) || ExtractorLogic.MOST.equals(modifier)) {
					final Function<SortedSet<Rendition>, Rendition> extractor = //
						(ExtractorLogic.LEAST.equals(modifier) ? SortedSet::first : SortedSet::last);
					p = p.and((rendition, idx) -> {
						SortedSet<Rendition> peers = idx.get(RenditionIndexType.MODIFIER, format);
						if ((peers == null) || peers.isEmpty()) { return true; }
						return Tools.equals(rendition, extractor.apply(peers));
					});
				} else {
					p = p.and((rendition, peers) -> Tools.equals(rendition.getModifier(), modifier));
				}
			}
		}

		// If an age modifier is specified, add it
		if ((age != null)
			&& (ExtractorLogic.OLDEST.equalsIgnoreCase(age) || ExtractorLogic.NEWEST.equalsIgnoreCase(age))) {
			final Function<SortedSet<Rendition>, Rendition> extractor = //
				(ExtractorLogic.OLDEST.equalsIgnoreCase(modifier) ? SortedSet::first : SortedSet::last);
			p = p.and((rendition, index) -> {
				SortedSet<Rendition> peers = index.get(RenditionIndexType.DATE, format);
				if ((peers == null) || peers.isEmpty()) { return true; }
				return Tools.equals(rendition, extractor.apply(peers));
			});
		}

		return p;
	}

	static BiFunction<Rendition, RenditionIndexGroup, Integer> compileRenditionPrioritizer(Collection<String> strings) {
		if ((strings == null) || strings.isEmpty()) { return null; }
		final Collection<BiPredicate<Rendition, RenditionIndexGroup>> predicates = new ArrayList<>(strings.size());
		strings.stream()//
			.filter(StringUtils::isNotBlank)//
			.map(ExtractorLogic::compilePrioritizer)//
			.filter(Objects::nonNull)//
			.forEach(predicates::add) //
		;

		if (predicates.isEmpty()) { return null; }

		return (rendition, peers) -> {
			int pos = 0;
			for (BiPredicate<Rendition, RenditionIndexGroup> p : predicates) {
				if (p.test(rendition, peers)) { return pos; }
				pos++;
			}
			// We have predicates but none matched, so only output the primary
			if (rendition.getType() == 0) { return Integer.MAX_VALUE; }
			return null;
		};
	}

	private final DfcSessionPool pool;
	private final Predicate<Content> contentFilter;
	private final Predicate<Rendition> renditionFilter;
	private final Consumer<Content> contentConsumer;
	private final BiFunction<Rendition, RenditionIndexGroup, Integer> renditionPrioritizer;

	public ExtractorLogic(DfcSessionPool pool, Consumer<Content> contentConsumer, String contentFilter,
		String renditionFilter, Collection<String> renditionPrioritizer) throws ScriptException {
		this.contentFilter = ExtractorLogic.compileFilter(Content.class, contentFilter);
		this.renditionFilter = ExtractorLogic.compileFilter(Rendition.class, renditionFilter);
		this.renditionPrioritizer = ExtractorLogic.compileRenditionPrioritizer(renditionPrioritizer);
		this.pool = pool;
		this.contentConsumer = contentConsumer;
	}

	@Override
	public IDfSession initialize() throws DfException {
		return this.pool.acquireSession();
	}

	@Override
	public void process(IDfSession session, IDfId id) throws Exception {
		if (id == null) { return; }
		final IDfLocalTransaction tx = DfcUtils.openTransaction(session);
		try {
			Content c = getContent(session, id);
			if (c == null) { return; }
			if ((this.contentFilter == null) || this.contentFilter.test(c)) {
				this.contentConsumer.accept(c);
			}
		} finally {
			try {
				// No matter what...roll back!
				DfcUtils.abortTransaction(session, tx);
			} catch (DfException e) {
				this.log.warn("Could not abort an open transaction", e);
			}
		}
	}

	private IDfSysObject getSysObject(IDfSession session, IDfId id) throws DfException {
		final IDfPersistentObject object;
		try {
			object = session.getObject(id);
		} catch (DfIdNotFoundException e) {
			return null;
		}

		if (object.isInstanceOf("dm_folder")) { return null; }
		if (!object.isInstanceOf("dm_sysobject")) { return null; }

		return IDfSysObject.class.cast(object);

	}

	private void findObjectPaths(IDfSession session, IDfSysObject document, Consumer<String> target)
		throws DfException {
		String objectName = document.getObjectName();
		if (StringUtils.isBlank(objectName)) {
			objectName = String.format("(blank-object-name-%s)", document.getObjectId().getId());
		}
		if (document.getHasFolder()) {
			final int pathCount = document.getFolderIdCount();
			for (int i = 0; i < pathCount; i++) {
				IDfId parentId = document.getFolderId(i);
				IDfFolder parent = session.getFolderBySpecification(parentId.getId());
				if (parent != null) {
					final int parentPathCount = parent.getFolderPathCount();
					for (int j = 0; j < parentPathCount; j++) {
						target.accept(String.format("%s/%s", parent.getFolderPath(j), document.getObjectName()));
					}
				} else {
					target.accept(String.format("<parent-%s-not-found>/%s", parentId, objectName));
				}
			}
		} else {
			target.accept(String.format("(unfiled)/%s", objectName));
		}
	}

	private Integer calculatePriority(Rendition rendition, RenditionIndexGroup indexer) {
		if (this.renditionPrioritizer == null) { return null; }
		return this.renditionPrioritizer.apply(rendition, indexer);
	}

	private Rendition selectBestRendition(Collection<Rendition> renditions) {
		// If there's no selection to be made, then we make none
		if ((this.renditionFilter == null) && (this.renditionPrioritizer == null)) { return null; }

		// Remove renditions that have been explicitly filtered out
		if (this.renditionFilter != null) {
			renditions.removeIf(this.renditionFilter.negate());
		}

		if (this.renditionPrioritizer != null) {
			// Calculate the preferred rendition
			RenditionIndexGroup indexer = new RenditionIndexGroup(renditions, RenditionIndexType.values());
			Pair<Integer, Rendition> best = null;
			for (Rendition r : renditions) {
				Integer newPriority = calculatePriority(r, indexer);
				if (newPriority == null) {
					// We're not interested in this rendition
					continue;
				}
				if ((best == null) || (newPriority < best.getLeft())) {
					// This is either the first rendition that qualifies, or the best one so far
					best = Pair.of(newPriority, r);
				}
			}

			// Did we find a winner?
			if (best != null) { return best.getRight(); }
		}

		// No winner, so keep whatever's left after applying the filter
		return null;
	}

	private void saveRendition(Consumer<Rendition> target, Rendition rendition) {
		if (rendition == null) { return; }
		rendition.setPageCount(rendition.getPages().size());
		target.accept(rendition);
	}

	private Long findRenditions(IDfSession session, IDfSysObject document, Consumer<Rendition> target)
		throws DfException {
		// Calculate both the path and the ticket location
		final String dql = "" //
			+ "select dcs.r_object_id " //
			+ "  from dmr_content_r dcr, dmr_content_s dcs " //
			+ " where dcr.r_object_id = dcs.r_object_id " //
			+ "   and dcr.parent_id = %s " //
			+ " order by dcs.rendition, dcs.full_format, dcr.page_modifier, dcr.page ";

		int index = 0;
		final IDfId id = document.getObjectId();
		Long maxRendition = null;
		try (DfcQuery query = new DfcQuery(session, String.format(dql, DfcUtils.quoteString(id.getId())),
			DfcQuery.Type.DF_EXECREAD_QUERY)) {

			Rendition rendition = null;
			while (query.hasNext()) {
				final IDfId contentId = query.next().getId("r_object_id");
				final int idx = (index++);
				final IDfContent content;
				try {
					content = IDfContent.class.cast(session.getObject(contentId));
				} catch (DfException e) {
					this.log.error("Failed to retrieve the content stream object # {} (with id = [{}]) for {}", idx,
						contentId, id, e);
					continue;
				}

				if ((rendition == null) || !rendition.matches(content)) {
					saveRendition(target, rendition);
					rendition = new Rendition() //
						.setType(content.getRendition()) //
						.setFormat(content.getString("full_format")) //
						.setModifier(Tools.coalesce(content.getString("page_modifier"), "")) //
						.setDate(content.getSetTime().getDate()) //
					;
				}

				rendition.getPages().add(new Page() //
					.setContentId(content.getObjectId().getId()) //
					.setNumber(content.getInt("page")) //
					.setLength(content.getContentSize()) //
					.setHash(content.getContentHash()) //
					.setPath(DfcUtils.getContentLocation(session, content)));
			}
			saveRendition(target, rendition);
			return maxRendition;
		}
	}

	protected final Content getContent(IDfSession session, IDfId id) throws DfException {
		IDfSysObject document = getSysObject(session, id);
		if (document == null) { return null; }
		Content c = new Content() //
			.setId(id.getId()) //
			.setHistoryId(document.getChronicleId().getId()) //
			.setVersion(document.getVersionLabel(0)) //
			.setCurrent(document.getHasFolder()) //
		;
		findObjectPaths(session, document, c.getPaths()::add);
		findRenditions(session, document, c.getRenditions()::add);

		Rendition r = selectBestRendition(c.getRenditions());
		if (r != null) {
			// If a single rendition was selected, then keep only it
			c.getRenditions().clear();
			c.getRenditions().add(r);
		}
		return c;
	}

	@Override
	public void handleFailure(IDfSession session, IDfId id, Exception raised) {
		this.log.error("Failed to retrieve the content data for [{}]", id, raised);
	}

	@Override
	public void cleanup(IDfSession session) {
		this.pool.releaseSession(session);
	}
}