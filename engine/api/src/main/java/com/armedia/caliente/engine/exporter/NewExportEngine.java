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
package com.armedia.caliente.engine.exporter;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.armedia.caliente.engine.exporter.content.ContentExtractor;
import com.armedia.caliente.engine.exporter.locator.ExportTargetLocator;
import com.armedia.caliente.engine.exporter.locator.ExportTargetLocator.Search;
import com.armedia.caliente.engine.exporter.xml.ExtractSearchT;
import com.armedia.caliente.engine.exporter.xml.ExtractSourceT;
import com.armedia.caliente.engine.exporter.xml.Extraction;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.function.CheckedBiConsumer;
import com.armedia.commons.utilities.function.CheckedConsumer;

public abstract class NewExportEngine {

	protected static enum SearchType {
		//
		PATH, //
		KEY, //
		QUERY, //
		//
		;
	}

	protected abstract ExportTargetLocator<?> buildLocator(CfgTools cfg, String name, String engine)
		throws ExportException;

	protected List<Pair<String, Search>> buildSearchRunners(Extraction extraction) throws ExportException {
		List<ExtractSourceT> sources = extraction.getSources();
		Map<String, String> rootSettings = extraction.getSettings();
		final String sourceNameFormat = String.format("%%s@%%0%dd", String.valueOf(sources.size() + 1).length());
		List<Pair<String, Search>> runners = new LinkedList<>();
		int sourcePos = 0;
		for (ExtractSourceT source : sources) {
			sourcePos++;
			String engine = source.getEngine();
			String sourceName = source.getName();
			if (StringUtils.isBlank(sourceName)) {
				sourceName = String.format(sourceNameFormat, engine, sourcePos);
			}
			Map<String, String> settings = new LinkedHashMap<>(rootSettings);
			settings.putAll(source.getSettings());
			CfgTools cfg = new CfgTools(settings);

			ExportTargetLocator<?> locator = buildLocator(cfg, sourceName, engine);

			// Capture all its search runners:
			List<ExtractSearchT> searches = source.getSearches();
			final String searchNameFormat = String.format("%s#%%0%dd", sourceName,
				String.valueOf(searches.size() + 1).length());
			int searchPos = 0;
			for (ExtractSearchT search : searches) {
				searchPos++;
				String searchName = search.getName();
				if (StringUtils.isBlank(searchName)) {
					searchName = String.format(searchNameFormat, searchPos);
				} else {
					searchName = sourceName + "#" + searchName;
				}
				// TODO: Support script execution via "lang"?
				runners.add(Pair.of(searchName, locator.getSearchRunner(search.getValue())));
			}
		}
		return runners;
	}

	protected MetadataExtractor<?, ?> buildMetadataExtractor(Extraction extraction, ContentExtractor contentExtractor)
		throws ExportException {
		return null;
	}

	protected ContentExtractor buildContentExtractor(Extraction extraction) throws ExportException {
		return null;
	}

	protected ExecutorService buildExecutor(Extraction extraction) {
		return null;
	}

	protected final void work(CmfObjectCounter<ExportResult> counter) throws ExportException, CmfStorageException {
		// We get this at the very top because if this fails, there's no point in continuing.
		Extraction extraction = new Extraction();

		ExecutorService executor = buildExecutor(extraction);

		// First: construct all the Locators
		List<Pair<String, Search>> searches = buildSearchRunners(extraction);

		CheckedConsumer<ExportTarget, ExportException> consumer = null;
		final CheckedBiConsumer<String, Search, Exception> directProcessor = (name, search) -> search
			.runSearch(consumer);

		// If we're using an executor in the background, then processor becomes...
		BiConsumer<String, Search> processor = directProcessor;

		if (executor != null) {
			processor = (name, search) -> executor.submit(() -> search.runSearch(consumer));
		}

		try (ContentExtractor content = buildContentExtractor(extraction)) {
			try (MetadataExtractor<?, ?> metadata = buildMetadataExtractor(extraction, content)) {
				for (Pair<String, Search> p : searches) {
					processor.accept(p.getKey(), p.getValue());
				}
			}
		} catch (Exception e) {
			throw new ExportException("Exception caught while performing the extraction", e);
		}
	}
}