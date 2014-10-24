/**
 *
 */

package com.delta.cmsmf.cms.storage.base;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.log4j.Logger;
import org.h2.tools.DeleteDbFiles;

import com.armedia.cmf.storage.CmsStorageException;
import com.armedia.cmf.storage.jdbc.JdbcCmsObjectStore;
import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.dslocator.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;

/**
 * @author diego
 *
 */
public class DefaultCmsObjectStore extends JdbcCmsObjectStore {

	private static final Logger LOG = Logger.getLogger(DefaultCmsObjectStore.class);

	private static DefaultCmsObjectStore INSTANCE = null;

	public static synchronized DefaultCmsObjectStore init(boolean clearData) throws CMSMFException {
		if (DefaultCmsObjectStore.INSTANCE != null) { return DefaultCmsObjectStore.INSTANCE; }
		String targetPath = Setting.DB_DIRECTORY.getString();
		File targetDirectory = null;
		try {
			targetDirectory = new File(targetPath).getCanonicalFile();
		} catch (IOException e) {
			throw new CMSMFException(String.format("Failed to canonicalize the path [%s]", targetPath), e);
		}

		final String jdbcUrl = StrSubstitutor.replace(Setting.JDBC_URL.getString(),
			Collections.singletonMap("target", targetDirectory.getAbsolutePath()));

		// If we're using H2, then we delete the DB if it's in file: protocol
		Pattern p = Pattern.compile("^jdbc:h2:(?:([^:]+):)?(.*)/([^/;]+)(?:;.*)?$");
		Matcher m = p.matcher(jdbcUrl);
		if (clearData && m.matches()) {
			String protocol = m.group(1);
			if ((protocol == null) || StringUtils.equalsIgnoreCase("file", protocol)) {
				String path = m.group(2);
				String dbName = m.group(3);
				DeleteDbFiles.execute(path, dbName, false);
			}
		}

		if (DefaultCmsObjectStore.LOG.isInfoEnabled()) {
			DefaultCmsObjectStore.LOG.info(String.format("State database will be stored at [%s]", jdbcUrl));
		}

		Map<String, String> conf = new HashMap<String, String>();
		conf.put("jdbc.driver", Setting.JDBC_DRIVER.getString());
		conf.put("jdbc.url", jdbcUrl);
		conf.put("jdbc.user", Setting.JDBC_USER.getString());
		conf.put("jdbc.password", Setting.JDBC_PASSWORD.getString());
		CfgTools settings = new CfgTools(conf);
		final DataSourceLocator locator = DataSourceLocator.getFirstLocatorFor(null);
		final DataSourceDescriptor<?> dataSourceDescriptor;
		try {
			dataSourceDescriptor = locator.locateDataSource(settings);
		} catch (Exception e) {
			throw new CMSMFException("Exception raised attempting to locate the data source", e);
		}

		try {
			DefaultCmsObjectStore.INSTANCE = new DefaultCmsObjectStore(dataSourceDescriptor, clearData);
		} catch (CmsStorageException e) {
			throw new CMSMFException("Failed to initialize the object store", e);
		}
		return DefaultCmsObjectStore.INSTANCE;
	}

	public static synchronized void terminate() {
		if (DefaultCmsObjectStore.INSTANCE == null) { return; }
		DefaultCmsObjectStore.terminate();
		DefaultCmsObjectStore.INSTANCE = null;
	}

	/**
	 * @param dataSource
	 * @param clearData
	 * @throws CMSMFException
	 */
	private DefaultCmsObjectStore(DataSourceDescriptor<?> dataSourceDescriptor, boolean clearData)
		throws CmsStorageException {
		super(dataSourceDescriptor, clearData);
		if (clearData) {
			clearAllObjects();
		}
	}

	@Override
	protected boolean doClose() {
		try {
			if (this.log.isInfoEnabled()) {
				this.log.info("Closing the state database connection pool");
			}
			getDataSourceDescriptor().close();
		} catch (Exception e) {
			this.log.warn("Failed to close the JDBC connection pool", e);
		}
		return true;
	}
}