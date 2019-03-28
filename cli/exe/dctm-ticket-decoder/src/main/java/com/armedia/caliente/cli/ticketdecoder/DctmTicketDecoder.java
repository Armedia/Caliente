package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ReadWriteSet;
import com.armedia.commons.utilities.function.CheckedPredicate;
import com.armedia.commons.utilities.line.LineScanner;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class DctmTicketDecoder {

	private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();
	private static final Pattern SIMPLE_PRIORITY_PARSER = Pattern
		.compile("^(?:([0-3]):)?([^\\[@]+)(?:\\[(.+)\\]|@(oldest|youngest))?$");

	private static final String OLDEST = "oldest";
	private static final String YOUNGEST = "youngest";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DfcLaunchHelper dfcLaunchHelper;
	private final ThreadsLaunchHelper threadHelper;

	public DctmTicketDecoder(DfcLaunchHelper dfcLaunchHelper, ThreadsLaunchHelper threadHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
		this.threadHelper = threadHelper;
	}

	private ContentFinder buildContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source,
		Consumer<IDfId> consumer) {
		if (source.startsWith("%")) { return new SingleContentFinder(pool, scannedIds, source, consumer); }
		if (source.startsWith("/")) { return new PathContentFinder(pool, scannedIds, source, consumer); }
		return new PredicateContentFinder(pool, scannedIds, source, consumer);
	}

	private boolean isNaturalNumber(Number n) {
		if (Byte.class.isInstance(n)) { return true; }
		if (Short.class.isInstance(n)) { return true; }
		if (Integer.class.isInstance(n)) { return true; }
		if (Long.class.isInstance(n)) { return true; }
		if (BigInteger.class.isInstance(n)) { return true; }
		return false;
	}

	private boolean coerceBooleanResult(Object o) {
		// If it's a null, then it's a false right away
		if (o == null) { return false; }

		// Is it a boolean? Sweet!
		if (Boolean.class.isInstance(o)) { return Boolean.class.cast(o); }

		// Is it a number? 0 == false, non-0 == true
		if (Number.class.isInstance(o)) {
			Number n = Number.class.cast(o);
			if (isNaturalNumber(n)) {
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

	private <T> CheckedPredicate<T, ScriptException> compileFilter(Class<T> klazz, final String expression)
		throws ScriptException {
		CheckedPredicate<T, ScriptException> p = klazz::isInstance;
		if (expression != null) {
			// Compile the script
			ScriptEngine engine = DctmTicketDecoder.ENGINE_MANAGER.getEngineByName("jexl");
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
						return coerceBooleanResult(script.eval(b));
					};
				} else {
					scriptPredicate = (obj) -> {
						final Bindings b = new SimpleBindings();
						b.put(varName, obj);
						return coerceBooleanResult(engine.eval(expression, b));
					};
				}
				p = p.and(scriptPredicate);
			}
		}
		return p;
	}

	private BiPredicate<Rendition, Map<String, SortedSet<Rendition>>> compilePrioritizer(String priority) {
		Matcher m = DctmTicketDecoder.SIMPLE_PRIORITY_PARSER.matcher(priority);
		if (!m.matches()) { return null; }
		BiPredicate<Rendition, Map<String, SortedSet<Rendition>>> p = (rendition, peers) -> (rendition != null);
		final String type = m.group(1);
		final String format = m.group(2);
		final String modifier = m.group(3);
		final String age = m.group(4);

		// If a rendition type is specified, add it
		if (type != null) {
			try {
				final int t = Integer.parseInt(type);
				p = p.and((rendition, peers) -> rendition.getType() == t);
			} catch (NumberFormatException e) {
				// Invalid integer...
				return null;
			}
		}

		// Add the format
		if (format != null) {
			p = p.and((rendition, peers) -> Tools.equals(rendition.getFormat(), format));

			// If a modifier is specified, add it
			if (modifier != null) {
				p = p.and((rendition, peers) -> Tools.equals(rendition.getModifier(), modifier));
			}
		}

		// If an age modifier is specified, add it
		if (age != null) {
			final Function<Map<String, SortedSet<Rendition>>, SortedSet<Rendition>> candidateSelector;
			if (format != null) {
				candidateSelector = (map) -> map.get(format);
			} else {
				candidateSelector = (map) -> map.get(ExtractorLogic.ALL_MARKER);
			}
			final Function<SortedSet<Rendition>, Rendition> extractor;
			switch (StringUtils.lowerCase(age)) {
				case OLDEST:
					// Is this the oldest rendition for its format group?
					extractor = SortedSet::last;
					break;

				case YOUNGEST:
				default:
					// TODO: How?!? We need something to compare this rendition's date to...
					extractor = SortedSet::first;
					break;
			}
			p = p.and((rendition, map) -> {
				SortedSet<Rendition> peers = candidateSelector.apply(map);
				if ((peers == null) || peers.isEmpty()) { return false; }
				Rendition other = extractor.apply(peers);
				return Tools.equals(rendition, other);
			});
		}
		return p;
	}

	private BiFunction<Rendition, Map<String, SortedSet<Rendition>>, Integer> compileRenditionPrioritizer(
		Collection<String> strings) {
		if ((strings == null) || strings.isEmpty()) { return null; }
		final Collection<BiPredicate<Rendition, Map<String, SortedSet<Rendition>>>> predicates = new ArrayList<>(
			strings.size());
		strings.stream()//
			.filter(StringUtils::isNotBlank)//
			.map(this::compilePrioritizer)//
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

	protected int run(OptionValues cli) throws Exception {
		// final boolean debug = cli.isPresent(CLIParam.debug);
		final Collection<String> sources = cli.getStrings(CLIParam.from);
		final PersistenceFormat format = cli.getEnum(PersistenceFormat.class, CLIParam.format);
		final File target = Tools.canonicalize(new File(cli.getString(CLIParam.target)));
		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String user = this.dfcLaunchHelper.getDfcUser(cli);
		final String password = this.dfcLaunchHelper.getDfcPassword(cli);
		final int threads = this.threadHelper.getThreads(cli);

		final Predicate<Content> contentFilter = compileFilter(Content.class, cli.getString(CLIParam.content_filter));
		final Predicate<Rendition> renditionFilter = compileFilter(Rendition.class,
			cli.getString(CLIParam.rendition_filter));
		final BiFunction<Rendition, Map<String, SortedSet<Rendition>>, Integer> renditionPrioritizer = compileRenditionPrioritizer(
			cli.getStrings(CLIParam.prefer_rendition));

		final CloseableIterator<String> sourceIterator = new LineScanner().iterator(sources);

		final DfcSessionPool pool;
		try {
			pool = new DfcSessionPool(docbase, user, new DctmCrypto().decrypt(password));
		} catch (DfException e) {
			this.log.error("Failed to create the DFC session pool", e);
			return 1;
		}

		final BlockingQueue<Content> contents = new LinkedBlockingQueue<>();
		final AtomicBoolean running = new AtomicBoolean(true);
		Thread persistenceThread = null;

		final Set<String> scannedIds = new ReadWriteSet<>(new HashSet<>());

		int ret = 1;
		try (Stream<String> sourceStream = sourceIterator.stream()) {
			final PooledWorkers<IDfSession, IDfId> extractors = new PooledWorkers<>();
			final PooledWorkersLogic<IDfSession, IDfId, Exception> extractorLogic = new ExtractorLogic(pool, (c) -> {
				try {
					this.log.debug("Queueing {}", c);
					contents.put(c);
				} catch (InterruptedException e) {
					this.log.error("Failed to queue Content object {}", c, e);
				}
			}, contentFilter, renditionFilter, renditionPrioritizer);

			;
			final AtomicLong submittedCounter = new AtomicLong(0);
			final AtomicLong submitFailedCounter = new AtomicLong(0);
			final AtomicLong renderedCounter = new AtomicLong(0);
			final AtomicLong renderFailedCounter = new AtomicLong(0);
			try (ContentPersistor persistor = format.newPersistor()) {
				persistor.initialize(target);

				final Set<String> submittedSources = new HashSet<>();
				this.log.info("Starting the background searches...");
				extractors.start(extractorLogic, Math.max(1, threads), "Extractor", true);
				try {
					persistenceThread = new Thread(() -> {
						while (true) {
							final Content c;
							try {
								if (running.get()) {
									c = contents.take();
								} else {
									c = contents.poll();
								}
								if (c == null) { return; }
							} catch (InterruptedException e) {
								continue;
							}
							this.log.info("{}", c);
							try {
								persistor.persist(c);
								renderedCounter.incrementAndGet();
							} catch (Exception e) {
								renderFailedCounter.incrementAndGet();
								this.log.error("Failed to marshal the content object {}", c, e);
							}
						}
					});
					persistenceThread.setDaemon(true);
					running.set(true);
					persistenceThread.start();

					sourceStream //
						.filter((source) -> submittedSources.add(source)) //
						.forEach((source) -> {
							try {
								buildContentFinder(pool, scannedIds, source, (id) -> {
									try {
										this.log.debug("Submitting {}", id);
										extractors.addWorkItem(id);
										submittedCounter.incrementAndGet();
									} catch (InterruptedException e) {
										submitFailedCounter.incrementAndGet();
										this.log.error("Failed to add ID [{}] to the work queue", id, e);
									}
								}).call();
							} catch (Exception e) {
								this.log.error("Failed to search for elements from the source [{}]", source, e);
							}
						}) //
					;
					this.log.info("Finished searching from {} source{}...", submittedSources.size(),
						submittedSources.size() > 1 ? "s" : "");
					this.log.info(
						"Submitted a total of {} work items for extraction from ({} failed), waiting for generation to conclude...",
						submittedCounter.get(), submitFailedCounter.get());
				} finally {
					extractors.waitForCompletion();
					this.log.info("Object retrieval is complete, waiting for XML generation to finish...");
					if (persistenceThread != null) {
						running.set(false);
						persistenceThread.interrupt();
					}
					persistenceThread.join();
					this.log.info("Generated a total of {} content elements ({} failed) from the {} submitted",
						renderedCounter.get(), renderFailedCounter.get(), submittedCounter.get());
				}
			}
			ret = 0;
		} finally {
			pool.close();
		}
		return ret;
	}
}