package com.armedia.caliente.cli.ticketdecoder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
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
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DctmQuery;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedPredicate;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class ExtractorLogic implements PooledWorkersLogic<IDfSession, IDfId, Exception> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static final Comparator<Rendition> RENDITION_BY_DATE_OLD_TO_NEW = (a, b) -> a.getDate()
		.compareTo(b.getDate());
	private static final String ALL_MARKER = (UUID.randomUUID().toString() + UUID.randomUUID().toString());
	private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();
	private static final Pattern SIMPLE_PRIORITY_PARSER = Pattern
		.compile("^(?:([0-3]{1,4}):)?(?:([*]|[^*:@%]+)(?:%(.*?))?)?(?:@(old|new)(?:est)?)?$");
	private static final String TYPE_CANDIDATES = "0123";
	private static final String FORMAT_WILDCARD = "*";

	private static final String OLDEST = "old";
	private static final String NEWEST = "new";

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

	private static BiPredicate<Rendition, Map<String, SortedSet<Rendition>>> compilePrioritizer(String priority) {
		Matcher m = ExtractorLogic.SIMPLE_PRIORITY_PARSER.matcher(priority);
		if (!m.matches()) { return null; }
		BiPredicate<Rendition, Map<String, SortedSet<Rendition>>> p = (rendition, peers) -> (rendition != null);
		final String type = m.group(1);
		final String format = m.group(2);
		final String modifier = m.group(3);
		final String age = m.group(4);

		// If a rendition type is specified, add it
		if (type != null) {
			final boolean[] types = new boolean[ExtractorLogic.TYPE_CANDIDATES.length()];
			Arrays.fill(types, false); // Make sure...
			for (int i = 0; i < type.length(); i++) {
				try {
					final int t = Integer.parseInt(type.substring(i, i + 1));
					if ((t < 0) || (t > types.length)) {
						continue;
					}
					types[t] = true;
				} catch (NumberFormatException e) {
					continue;
				}
			}
			p = p.and((rendition, peers) -> types[rendition.getType() % types.length]);
		}

		boolean formatLimited = false;
		// Add the format
		if (format != null) {
			// If we're not using a format wildcard...
			if (!StringUtils.equals(ExtractorLogic.FORMAT_WILDCARD, format)) {
				p = p.and((rendition, peers) -> Tools.equals(rendition.getFormat(), format));
				formatLimited = true;
			}
			// If a modifier is specified, add it
			if (modifier != null) {
				p = p.and((rendition, peers) -> Tools.equals(rendition.getModifier(), modifier));
			}
		}

		// If an age modifier is specified, add it
		if (age != null) {
			final Function<Map<String, SortedSet<Rendition>>, SortedSet<Rendition>> candidateSelector;
			if (formatLimited) {
				candidateSelector = (map) -> map.get(format);
			} else {
				candidateSelector = (map) -> map.get(ExtractorLogic.ALL_MARKER);
			}
			final Function<SortedSet<Rendition>, Rendition> extractor;
			switch (StringUtils.lowerCase(age)) {
				case OLDEST:
					// Is this the oldest rendition for its format group?
					extractor = SortedSet::first;
					break;

				case NEWEST:
				default:
					// TODO: How?!? We need something to compare this rendition's date to...
					extractor = SortedSet::last;
					break;
			}
			p = p.and((rendition, map) -> {
				SortedSet<Rendition> peers = candidateSelector.apply(map);
				if ((peers == null) || peers.isEmpty()) { return true; }
				Rendition other = extractor.apply(peers);
				return Tools.equals(rendition, other);
			});
		}

		return p;
	}

	static BiFunction<Rendition, Map<String, SortedSet<Rendition>>, Integer> compileRenditionPrioritizer(
		Collection<String> strings) {
		if ((strings == null) || strings.isEmpty()) { return null; }
		final Collection<BiPredicate<Rendition, Map<String, SortedSet<Rendition>>>> predicates = new ArrayList<>(
			strings.size());
		strings.stream()//
			.filter(StringUtils::isNotBlank)//
			.map(ExtractorLogic::compilePrioritizer)//
			.filter(Objects::nonNull)//
			.forEach(predicates::add) //
		;

		if (predicates.isEmpty()) { return null; }

		return (rendition, peers) -> {
			int pos = 0;
			for (BiPredicate<Rendition, Map<String, SortedSet<Rendition>>> p : predicates) {
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
	private final BiFunction<Rendition, Map<String, SortedSet<Rendition>>, Integer> renditionPrioritizer;

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
		final IDfLocalTransaction tx = DfUtils.openTransaction(session);
		try {
			Content c = getContent(session, id);
			if (c == null) { return; }
			if ((this.contentFilter == null) || this.contentFilter.test(c)) {
				this.contentConsumer.accept(c);
			}
		} finally {
			try {
				// No matter what...roll back!
				DfUtils.abortTransaction(session, tx);
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

	public String getExtension(IDfSession session, IDfId format) throws DfException {
		if ((format == null) || format.isNull() || !format.isObjectId()) { return null; }
		IDfPersistentObject obj = session.getObject(format);
		if (!obj.isInstanceOf("dm_format")) { return null; }
		IDfFormat f = IDfFormat.class.cast(obj);
		return f.getDOSExtension();
	}

	public String getFileStoreLocation(IDfSession session, IDfContent content) throws DfException {
		try {
			IDfPersistentObject obj = session.getObject(content.getStorageId());
			String root = obj.getString("root");
			if (!StringUtils.isBlank(root)) {
				obj = session.getObjectByQualification(
					String.format("dm_location where object_name = %s", DfUtils.quoteString(root)));
				if ((obj != null) && obj.hasAttr("file_system_path")) { return obj.getString("file_system_path"); }
			}
			return String.format("(no-path-for-store-%s)", content.getStorageId());
		} catch (DfIdNotFoundException e) {
			return String.format("(store-%s-not-found)", content.getStorageId());
		}
	}

	private Integer calculatePriority(Rendition rendition, Map<String, SortedSet<Rendition>> peers) {
		if (this.renditionPrioritizer == null) { return null; }
		return this.renditionPrioritizer.apply(rendition, peers);
	}

	private Rendition selectRendition(Collection<Rendition> renditions) {
		// If there's no selection to be made, then we make none
		if ((this.renditionFilter == null) && (this.renditionPrioritizer == null)) { return null; }

		// Remove renditions that have been explicitly filtered out
		if (this.renditionFilter != null) {
			renditions.removeIf(this.renditionFilter.negate());
		}

		if (this.renditionPrioritizer != null) {
			// Calculate the preferred rendition
			Map<String, SortedSet<Rendition>> byFormatAndDate = new HashMap<>();
			SortedSet<Rendition> all = new TreeSet<>(ExtractorLogic.RENDITION_BY_DATE_OLD_TO_NEW);
			byFormatAndDate.put(ExtractorLogic.ALL_MARKER, all);
			for (Rendition r : renditions) {
				SortedSet<Rendition> s = byFormatAndDate.get(r.getFormat());
				if (s == null) {
					s = new TreeSet<>(ExtractorLogic.RENDITION_BY_DATE_OLD_TO_NEW);
					byFormatAndDate.put(r.getFormat(), s);
				}
				all.add(r);
				s.add(r);
			}
			Pair<Integer, Rendition> best = null;
			for (Rendition r : renditions) {
				Integer newPriority = calculatePriority(r, byFormatAndDate);
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
		try (DctmQuery query = new DctmQuery(session, String.format(dql, DfUtils.quoteString(id.getId())),
			DctmQuery.Type.DF_EXECREAD_QUERY)) {

			final String prefix = DfUtils.getDocbasePrefix(session);
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
				String streamPath = DfUtils.decodeDataTicket(prefix, content.getDataTicket(), '/');
				String extension = getExtension(session, content.getFormatId());
				if (StringUtils.isBlank(extension)) {
					extension = StringUtils.EMPTY;
				} else {
					extension = String.format(".%s", extension);
				}

				final String pathPrefix = getFileStoreLocation(session, content);

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
					.setNumber(content.getInt("page")) //
					.setLength(content.getContentSize()) //
					.setHash(content.getContentHash()) //
					.setPath(String.format("%s/%s%s", pathPrefix.replace('\\', '/'), streamPath, extension)));
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
		;
		findObjectPaths(session, document, c.getPaths()::add);
		findRenditions(session, document, c.getRenditions()::add);

		Rendition r = selectRendition(c.getRenditions());
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