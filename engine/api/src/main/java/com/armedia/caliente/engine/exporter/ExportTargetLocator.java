package com.armedia.caliente.engine.exporter;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

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

	public final Stream<ExportTarget> getExportTargets(String source) throws Exception {
		Objects.requireNonNull(source, "Must provide the search terms");
		final SearchType searchType = detectSearchType(source);
		if (searchType == null) {
			throw new ExportException(
				String.format("This engine doesn't know how to search for exportable objects using [%s]", source));
		}
		return getExportTargets(searchType, source);
	}

	public final Stream<ExportTarget> getExportTargets(SearchType searchType, String source) throws Exception {
		Objects.requireNonNull(searchType, "Must provide the type of search to execute");
		Objects.requireNonNull(source, "Must provide the search terms");
		if (!this.supportedSearches.contains(searchType)) {
			throw new ExportException(String.format("This engine doesn't support searches by %s (from the source [%s])",
				searchType.name().toLowerCase(), source));
		}

		SessionWrapper<SESSION> sessionWrapper = this.sessionFactory.acquireSession();
		final SESSION session = sessionWrapper.get();
		Stream<ExportTarget> ret = null;
		switch (searchType) {
			case KEY:
				// SearchKey!
				final String searchKey = StringUtils.strip(source.substring(1));
				if (StringUtils.isEmpty(searchKey)) {
					throw new ExportException(
						String.format("Invalid search key [%s] - no object can be found with an empty key"));
				}
				ret = findExportTargetsBySearchKey(session, searchKey);
				break;
			case PATH:
				// CMS Path!
				ret = findExportTargetsByPath(session, source);
				break;
			case QUERY:
				// Query string!
				ret = findExportTargetsByQuery(session, source);
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

}