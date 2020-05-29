package com.armedia.caliente.engine.exporter;

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

	protected abstract Stream<ExportTarget> findExportTargetsByQuery(SESSION session, CfgTools configuration,
		String query) throws Exception;

	protected abstract Stream<ExportTarget> findExportTargetsByPath(SESSION session, CfgTools configuration,
		String path) throws Exception;

	protected abstract Stream<ExportTarget> findExportTargetsBySearchKey(SESSION session, CfgTools configuration,
		String searchKey) throws Exception;

	public final Stream<ExportTarget> getExportTargets(String source) throws Exception {
		final SearchType searchType = detectSearchType(source);
		if (searchType == null) {
			throw new ExportException(
				String.format("This engine doesn't know how to search for exportable objects using [%s]", source));
		}

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
				ret = findExportTargetsBySearchKey(session, this.settings, searchKey);
				break;
			case PATH:
				// CMS Path!
				ret = findExportTargetsByPath(session, this.settings, source);
				break;
			case QUERY:
				// Query string!
				ret = findExportTargetsByQuery(session, this.settings, source);
			default:
				break;
		}

		if (ret != null) {
			if (ret.isParallel()) {
				// Switch to sequential mode - we're doing our own parallelism here
				ret = ret.sequential();
			}
		} else {
			ret = Stream.empty();
		}

		// Make sure we only close the session after the search scan is complete
		return ret.onClose(sessionWrapper::close);
	}

}