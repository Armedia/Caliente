package com.armedia.caliente.engine.exporter.locator;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedSupplier;

public abstract class ExportTargetLocator<SESSION> {

	public static interface SearchRunner extends CheckedSupplier<Stream<ExportTarget>, Exception> {
	}

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
	 * Detect the type of search to conduct, or {@code null} if this search pattern isn't supported.
	 *
	 * @return the type of search to conduct, or {@code null} if this search pattern isn't
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
		Objects.requireNonNull(searchType, "Must provide the type of search to execute");
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
			try {
				return Stream.empty();
			} finally {
				sessionWrapper.close();
			}
		}

		if (ret.isParallel()) {
			// Switch to sequential mode - we're doing our own parallelism here
			ret = ret.sequential();
		}

		// Make sure we close the session after the scan is complete
		ret.onClose(sessionWrapper::close);

		return ret;
	}

	public SearchRunner getSearchRunner(SearchType searchType, String term) {
		Objects.requireNonNull(term, "Must provide the search term");
		final SearchType type = Optional.ofNullable(searchType).orElseGet(() -> detectSearchType(term));
		return () -> findExportTargets(type, term);
	}
}