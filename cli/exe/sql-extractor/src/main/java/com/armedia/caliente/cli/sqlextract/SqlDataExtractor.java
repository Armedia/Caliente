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
package com.armedia.caliente.cli.sqlextract;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.function.Supplier;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.utils.CliValuePrompt;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.commons.utilities.Tools;

public class SqlDataExtractor {

	private final Logger console = LoggerFactory.getLogger("console");
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ThreadsLaunchHelper threadsLaunchHelper;

	public SqlDataExtractor(ThreadsLaunchHelper threadsLaunchHelper) {
		this.threadsLaunchHelper = threadsLaunchHelper;
	}

	private String getPassword(String user, Supplier<Option> option, OptionValues cli) {
		if (user == null) { return cli.getString(option); }
		return CliValuePrompt.getPasswordString(cli, CLIParam.password, "Please enter the Password for user [%s]: ",
			user);
	}

	protected int run(OptionValues cli, Collection<String> positionals) throws Exception {
		// final boolean debug = cli.isPresent(CLIParam.debug);
		final ResultsFormat format = cli.getEnum(ResultsFormat.class, CLIParam.format);
		final File target = Tools.canonicalize(new File(cli.getString(CLIParam.target)));

		final String url = cli.getString(CLIParam.url);
		final String user = cli.getString(CLIParam.user);
		final String password = getPassword(user, CLIParam.password, cli);

		final File properties = Tools.canonicalize(new File(cli.getString(CLIParam.properties)));

		final int threads = this.threadsLaunchHelper.getThreads(cli);
		final BasicDataSource dataSource;
		dataSource = new BasicDataSource();
		dataSource.setUrl(url);
		if (!StringUtils.isEmpty(user)) {
			dataSource.setUsername(user);
		}
		if (!StringUtils.isEmpty(password)) {
			dataSource.setPassword(password);
		}

		int ret = 1;
		try {
			try (Connection c = dataSource.getConnection()) {
			} catch (SQLException e) {
				this.log.error("Failed to create the JDBC session pool", e);
				return 1;
			}
			try {
				ret = 0;

			} finally {

			}
			return ret;
		} finally {
			dataSource.close();
		}
	}
}