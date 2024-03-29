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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.dfc.DfcCrypto;
import com.armedia.caliente.tools.dfc.cli.DfcLaunchHelper;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.utils.ThreadsLaunchHelper;
import com.armedia.commons.utilities.concurrent.ShareableCollection;
import com.armedia.commons.utilities.concurrent.ShareableSet;
import com.armedia.commons.utilities.line.LineScanner;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class DctmTicketDecoder {

	protected static final Pattern OUTPUT_PARSER = Pattern.compile("^([^@]+)(?:@(.+))?$");

	private static final String DEFAULT_TARGET = "content-tickets";

	private final Logger console = LoggerFactory.getLogger("console");
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ThreadGroup threadGroup = new ThreadGroup("AsyncPersistors");

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

	private ContentPersistor buildPersistor(PersistenceFormat format, File target) throws Exception {
		return Objects.requireNonNull(format).newPersistor(Objects.requireNonNull(target));
	}

	private ContentPersistor buildPersistor(Map<PersistenceFormat, File> outputInfo) throws Exception {
		if (outputInfo.size() == 1) {
			Map.Entry<PersistenceFormat, File> e = outputInfo.entrySet().iterator().next();
			return buildPersistor(e.getKey(), e.getValue());
		}

		// Ok...so we have multiple persistors. We'll need one thread per initialized persistor...
		final Collection<ContentPersistor> persistors = new ArrayList<>();
		for (PersistenceFormat format : outputInfo.keySet()) {
			File target = outputInfo.get(format);
			try {
				persistors.add(new AsyncContentPersistorWrapper(this.threadGroup, format.newPersistor(target)));
			} catch (Exception e) {
				this.console.error("Failed to initialize the {} persistor to [{}]", format.name(), target, e);
				continue;
			}
		}

		return new DelegatingContentPersistor(persistors);
	}

	protected int run(OptionValues cli) throws Exception {
		// final boolean debug = cli.isPresent(CLIParam.debug);
		final Collection<String> sources = cli.getStrings(CLIParam.from);

		Map<PersistenceFormat, File> outputInfo = new EnumMap<>(PersistenceFormat.class);
		for (String o : cli.getStrings(CLIParam.output)) {
			Matcher m = DctmTicketDecoder.OUTPUT_PARSER.matcher(o);
			if (!m.matches()) {
				// How is this possible?
			}
			final PersistenceFormat format;
			try {
				format = PersistenceFormat.decode(m.group(1));
			} catch (IllegalArgumentException e) {
				this.console.error("Invalid output format [{}] given", m.group(1));
				continue;
			}
			String target = m.group(2);
			if (outputInfo.containsKey(format)) {
				File f = outputInfo.get(format);
				this.console.warn("Duplicate output format {} - already writing it to [{}] - will not output to [{}]",
					format, f, target);
				continue;
			}

			if (StringUtils.isEmpty(target)) {
				target = String.format("%s.%s", DctmTicketDecoder.DEFAULT_TARGET, format.name().toLowerCase());
			}
			File file = Tools.canonicalize(new File(target));
			outputInfo.put(format, file);
		}
		if (outputInfo.isEmpty()) {
			this.console.error("No valid outputs found, cannot continue");
			return 1;
		}
		outputInfo = Tools.freezeMap(outputInfo);

		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String user = this.dfcLaunchHelper.getDfcUser(cli);
		final String password = this.dfcLaunchHelper.getDfcPassword(cli);

		final DfcSessionPool pool;
		try {
			pool = new DfcSessionPool(docbase, user, DfcCrypto.INSTANCE.decrypt(password));
		} catch (DfException e) {
			this.log.error("Failed to create the DFC session pool", e);
			return 1;
		}

		final int threads = this.threadHelper.getThreads(cli);
		final CloseableIterator<String> sourceIterator = new LineScanner().iterator(sources);

		final Set<String> scannedIds = new ShareableSet<>(new HashSet<>());

		int ret = 1;
		try (Stream<String> sourceStream = sourceIterator.stream()) {
			final AtomicLong submittedCounter = new AtomicLong(0);
			final AtomicLong outputCounter = new AtomicLong(0);
			final Collection<Pair<IDfId, Exception>> failedSubmissions = new ShareableCollection<>(new LinkedList<>());

			try (ContentPersistor persistor = new AsyncContentPersistorWrapper(this.threadGroup,
				buildPersistor(outputInfo))) {
				persistor.initialize();
				final PooledWorkers<IDfSession, IDfId> extractors = //
					new PooledWorkers.Builder<IDfSession, IDfId, Exception>() //
						.logic( //
							new ExtractorLogic(pool //
								, (c) -> {
									this.console.info("Persisting {}", c);
									persistor.persist(c);
									outputCounter.incrementAndGet();
								} //
								, cli.getString(CLIParam.content_filter) //
								, cli.getString(CLIParam.rendition_filter) //
								, cli.getStrings(CLIParam.prefer_rendition) //
							) //
						) //
						.threads(threads) //
						.name("Extractor") //
						.waitForWork(true) //
						.start() //
				;
				try {
					final Set<String> submittedSources = new HashSet<>();
					this.console.info("Starting the background searches...");
					sourceStream //
						.filter((source) -> submittedSources.add(source)) //
						.forEach((source) -> {
							try {
								buildContentFinder(pool, scannedIds, source, (id) -> {
									try {
										this.console.info("Submitting {}", id);
										extractors.addWorkItem(id);
										submittedCounter.incrementAndGet();
									} catch (InterruptedException e) {
										failedSubmissions.add(Pair.of(id, e));
										this.log.error("Failed to add ID [{}] to the work queue", id, e);
									}
								}).call();
							} catch (Exception e) {
								this.log.error("Failed to search for elements from the source [{}]", source, e);
							}
						}) //
					;
					this.console.info("Finished searching from {} source{}...", submittedSources.size(),
						submittedSources.size() > 1 ? "s" : "");
					this.console.info(
						"Submitted a total of {} work items for extraction from ({} failed), waiting for generation to conclude...",
						submittedCounter.get(), failedSubmissions.size());
				} finally {
					this.console.info("Object retrieval is complete, will wait for the generators to finish");
					extractors.waitForCompletion();
				}
			} finally {
				this.console.info("Generated a total of {} content elements of the {} submitted", outputCounter.get(),
					submittedCounter.get());
				try {
					if (!failedSubmissions.isEmpty()) {
						this.log.error("SUBMISSION ERRORS:");
						failedSubmissions
							.forEach((p) -> this.log.error("Failed to submit the ID {}", p.getLeft(), p.getRight()));
					}
				} catch (Exception e) {
					this.log.error("UNABLE TO LOG {} SUBMISSION ERRORS", failedSubmissions.size());
				}
			}
			ret = 0;
		} finally {
			pool.close();
		}
		return ret;
	}
}