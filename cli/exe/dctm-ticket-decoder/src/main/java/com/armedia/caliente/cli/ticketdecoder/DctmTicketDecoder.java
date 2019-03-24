package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;
import com.armedia.commons.utilities.concurrent.ReadWriteSet;
import com.armedia.commons.utilities.function.CheckedLazySupplier;
import com.armedia.commons.utilities.line.LineScanner;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.documentum.fc.common.DfException;

import javanet.staxutils.IndentingXMLStreamWriter;

public class DctmTicketDecoder {

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

	private ContentFinder buildTicketDecoder(DfcSessionPool pool, Set<String> scannedIds, String source,
		Consumer<Content> consumer) {
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

	private Predicate<Rendition> compilePredicate(String expression) {
		return null;
	}

	protected int run(OptionValues cli, Collection<String> sources) throws Exception {
		final boolean debug = cli.isPresent(CLIParam.debug);
		final File target = Tools.canonicalize(new File(cli.getString(CLIParam.target)));
		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String user = this.dfcLaunchHelper.getDfcUser(cli);
		final String password = this.dfcLaunchHelper.getDfcPassword(cli);
		final int threads = this.threadHelper.getThreads(cli);

		final Predicate<Rendition> renditionPredicate = compilePredicate(cli.getString(CLIParam.rendition_filter));

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

		try (Stream<String> sourceStream = sourceIterator.stream()) {
			final List<Future<Void>> futures = new LinkedList<>();
			final ExecutorService executors = Executors.newFixedThreadPool(Math.max(1, threads));
			final Set<String> submittedSources = new HashSet<>();

			int ret = 0;
			this.log.info("Retrieving data from the background workers...");
			Marshaller marshaller = getMarshaller();
			try (OutputStream out = new FileOutputStream(target)) {
				XMLStreamWriter xmlWriter = startXml(out);

				xmlThread = new Thread(() -> {
					while (running.get()) {
						final Content c;
						try {
							c = contents.take();
						} catch (InterruptedException e) {
							continue;
						}
						this.log.info("{}", c);
						try {
							marshaller.marshal(c, xmlWriter);
						} catch (JAXBException e) {
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
						ContentFinder decoder = buildTicketDecoder(pool, scannedIds, source, (c) -> {
							try {
								contents.put(c);
							} catch (InterruptedException e) {
								this.log.error("Interrupted while trying to queue up a content object: {}", c, e);
							}
						});
						decoder.setRenditionPredicate(renditionPredicate);
						futures.add(executors.submit(decoder));
					}) //
				;

				this.log.info("Submitted {} history search{}...", submittedSources.size(),
					submittedSources.size() > 1 ? "es" : "");
				executors.shutdown();
				try {
					for (Future<Void> f : futures) {
						try {
							f.get();
						} catch (ExecutionException e) {
							Throwable cause = e.getCause();
							if (DctmTicketDecoderException.class.isInstance(cause)) {
								DctmTicketDecoderException he = DctmTicketDecoderException.class.cast(cause);
								if (debug) {
									this.log.error(he.getMessage(), he.getCause());
								} else {
									this.log.error("{} (use --debug for more information)", he.getMessage());
								}
							} else {
								this.log.error("An unexpected exception was raised while reading a chronicle", cause);
							}
							ret = 1;
						}
					}
					return ret;
				} finally {
					endXml(xmlWriter);
					out.flush();
				}
			}
		} finally {
			if (xmlThread != null) {
				running.set(false);
				xmlThread.interrupt();
			}
			pool.close();
		}
	}
}