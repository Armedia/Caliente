package com.armedia.caliente.engine.exporter.locator;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedConsumer;

public abstract class ExportTargetLocator<SESSION> {

	public static enum SearchType {
		//
		PATH, //
		KEY, //
		QUERY, //
		//
		;
	}

	private final Set<SearchType> supportedSearches;
	private final SessionFactory<SESSION> sessionFactory;
	protected final CfgTools settings;

	public ExportTargetLocator(CfgTools settings, Set<SearchType> supportedSearches,
		SessionFactory<SESSION> sessionFactory) {
		this.settings = settings;
		this.supportedSearches = Tools.freezeCopy(supportedSearches);
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Detect the searchType of search to conduct, or {@code null} if this search pattern isn't
	 * supported.
	 *
	 * @return the searchType of search to conduct, or {@code null} if this search pattern isn't
	 *         supported.
	 */
	protected SearchType detectSearchType(String source) {
		if (source == null) { return null; }
		if (source.startsWith("%")) { return SearchType.KEY; }
		if (source.startsWith("/")) { return SearchType.PATH; }
		return SearchType.QUERY;
	}

	protected abstract Stream<ExportTarget> findExportTargetsByQuery(SESSION session, String query) throws Exception;

	protected abstract Stream<ExportTarget> findExportTargetsByPath(SESSION session, String path) throws Exception;

	protected abstract Stream<ExportTarget> findExportTargetsBySearchKey(SESSION session, String searchKey)
		throws Exception;

	private Stream<ExportTarget> findExportTargets(SearchType searchType, String term) throws Exception {
		Objects.requireNonNull(searchType, "Must provide the searchType of search to execute");
		Objects.requireNonNull(term, "Must provide the search term");
		if (!this.supportedSearches.contains(searchType)) {
			throw new ExportException(String.format("This engine doesn't support searches by %s (from the source [%s])",
				searchType.name().toLowerCase(), term));
		}

		SessionWrapper<SESSION> sessionWrapper = this.sessionFactory.acquireSession();
		final SESSION session = sessionWrapper.get();
		Stream<ExportTarget> ret = null;
		switch (searchType) {
			case KEY:
				// SearchKey!
				final String searchKey = StringUtils.strip(term.substring(1));
				if (StringUtils.isEmpty(searchKey)) {
					throw new ExportException(
						String.format("Invalid search key [%s] - no object can be found with an empty key"));
				}
				ret = findExportTargetsBySearchKey(session, searchKey);
				break;
			case PATH:
				// CMS Path!
				ret = findExportTargetsByPath(session, term);
				break;
			case QUERY:
				// Query string!
				ret = findExportTargetsByQuery(session, term);
			default:
				break;
		}

		// If there's nothing to return, always return an empty Stream
		if (ret == null) {
			sessionWrapper.close();
			return Stream.empty();
		}

		if (ret.isParallel()) {
			// Switch to sequential mode - we're doing our own parallelism here
			ret = ret.sequential();
		}
		ret = ret.filter(Objects::nonNull);

		// Make sure we close the session after the scan is complete
		ret.onClose(sessionWrapper::close);

		return ret;
	}

	public static class Search {
		private final ExportTargetLocator<?> locator;
		private final SearchType searchType;
		private final String term;

		private Search(ExportTargetLocator<?> locator, String term) {
			Objects.requireNonNull(term, "Must provide the search term");
			this.locator = Objects.requireNonNull(locator, "Must provide the locator to search with");
			this.searchType = locator.detectSearchType(term);
			this.term = term;
		}

		public long runSearch(CheckedConsumer<ExportTarget, ? extends Exception> consumer) throws Exception {
			Stream<ExportTarget> stream = this.locator.findExportTargets(this.searchType, this.term);
			if (consumer == null) { return stream.count(); }

			final AtomicLong counter = new AtomicLong();
			stream.forEach(consumer.andThen((t) -> counter.incrementAndGet()));
			return counter.get();
		}

		public ExportTargetLocator<?> getLocator() {
			return this.locator;
		}

		public SearchType getSearchType() {
			return this.searchType;
		}

		public String getTerm() {
			return this.term;
		}
	}

	public Search getSearchRunner(String term) {
		return new Search(this, term);
	}
}