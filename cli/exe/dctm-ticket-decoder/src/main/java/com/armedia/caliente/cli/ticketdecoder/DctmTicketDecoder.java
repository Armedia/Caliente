package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;
import com.armedia.commons.utilities.concurrent.ReadWriteSet;
import com.armedia.commons.utilities.function.CheckedLazySupplier;
import com.armedia.commons.utilities.function.CheckedPredicate;
import com.armedia.commons.utilities.line.LineScanner;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

import javanet.staxutils.IndentingXMLStreamWriter;

public class DctmTicketDecoder {

	private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();

	private static final CheckedLazySupplier<XMLOutputFactory, XMLStreamException> OUTPUT_FACTORY = new CheckedLazySupplier<>(
		() -> {
			WstxOutputFactory factory = new WstxOutputFactory();
			try {
				// This is only supported after 5.0
				Field f = WstxOutputProperties.class.getDeclaredField("P_USE_DOUBLE_QUOTES_IN_XML_DECL");
				if (Modifier.isStatic(f.getModifiers()) && String.class.isAssignableFrom(f.getType())) {
					Object v = f.get(null);
					if (v != null) {
						factory.setProperty(v.toString(), true);
					}
				}
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
				// It's ok...we're using an older version, so we simply won't have double quotes on
				// the XML declaration
			}
			factory.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES, true);
			factory.setProperty(WstxOutputProperties.P_ADD_SPACE_AFTER_EMPTY_ELEM, true);
			factory.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR, true);
			return factory;
		});

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

	private XMLStreamWriter startXml(OutputStream out) throws XMLStreamException {
		XMLOutputFactory factory = DctmTicketDecoder.OUTPUT_FACTORY.get();
		XMLStreamWriter writer = factory.createXMLStreamWriter(out);
		XMLStreamWriter xml = new IndentingXMLStreamWriter(writer) {
			@Override
			public NamespaceContext getNamespaceContext() {
				return XmlProperties.NO_NAMESPACES;
			}
		};
		xml.writeStartDocument(Charset.defaultCharset().name(), "1.1");
		String rootElement = "contents";
		xml.writeDTD(String.format("<!DOCTYPE %s>", rootElement));
		xml.writeStartElement(rootElement);
		writer.flush();
		return xml;
	}

	private Marshaller getMarshaller() throws JAXBException {
		Marshaller m = XmlTools.getMarshaller(Content.class, Rendition.class);
		m.setProperty(Marshaller.JAXB_ENCODING, Charset.defaultCharset().name());
		m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		return m;
	}

	private void endXml(XMLStreamWriter writer) throws XMLStreamException {
		writer.flush();
		writer.writeEndDocument();
		writer.close();
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

	private <T> CheckedPredicate<T, ScriptException> compilePredicate(Class<T> klazz, final String expression)
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

	protected int run(OptionValues cli, Collection<String> sources) throws Exception {
		// final boolean debug = cli.isPresent(CLIParam.debug);
		final File target = Tools.canonicalize(new File(cli.getString(CLIParam.target)));
		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String user = this.dfcLaunchHelper.getDfcUser(cli);
		final String password = this.dfcLaunchHelper.getDfcPassword(cli);
		final int threads = this.threadHelper.getThreads(cli);

		final Predicate<Content> contentPredicate = compilePredicate(Content.class,
			cli.getString(CLIParam.content_filter));
		final Predicate<Rendition> renditionPredicate = compilePredicate(Rendition.class,
			cli.getString(CLIParam.rendition_filter));

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
		Thread xmlThread = null;

		final Set<String> scannedIds = new ReadWriteSet<>(new HashSet<>());

		int ret = 1;
		try (Stream<String> sourceStream = sourceIterator.stream()) {
			final PooledWorkers<IDfSession, IDfId> extractors = new PooledWorkers<>();
			final PooledWorkersLogic<IDfSession, IDfId> extractorLogic = new ExtractorLogic(pool, (c) -> {
				try {
					this.log.debug("Queueing {}", c);
					contents.put(c);
				} catch (InterruptedException e) {
					this.log.error("Failed to queue Content object {}", c, e);
				}
			}, contentPredicate, renditionPredicate);

			Marshaller marshaller = getMarshaller();
			final AtomicLong submittedCounter = new AtomicLong(0);
			final AtomicLong submitFailedCounter = new AtomicLong(0);
			final AtomicLong renderedCounter = new AtomicLong(0);
			final AtomicLong renderFailedCounter = new AtomicLong(0);
			try (OutputStream out = new FileOutputStream(target)) {
				XMLStreamWriter xmlWriter = startXml(out);

				final Set<String> submittedSources = new HashSet<>();
				this.log.info("Starting the background searches...");
				extractors.start(extractorLogic, Math.max(1, threads), "Extractor", true);
				try {
					xmlThread = new Thread(() -> {
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
								marshaller.marshal(c, xmlWriter);
								renderedCounter.incrementAndGet();
							} catch (JAXBException e) {
								renderFailedCounter.incrementAndGet();
								this.log.error("Failed to marshal the content object {}", c, e);
							}
						}
					});
					xmlThread.setDaemon(true);
					running.set(true);
					xmlThread.start();

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
					if (xmlThread != null) {
						running.set(false);
						xmlThread.interrupt();
					}
					xmlThread.join();
					this.log.info("Generated a total of {} content elements ({} failed) from the {} submitted",
						renderedCounter.get(), renderFailedCounter.get(), submittedCounter.get());
					endXml(xmlWriter);
					out.flush();
				}
			}
			ret = 0;
		} finally {
			pool.close();
		}
		return ret;
	}
}