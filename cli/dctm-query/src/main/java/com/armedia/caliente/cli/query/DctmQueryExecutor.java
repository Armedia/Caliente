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
package com.armedia.caliente.cli.query;

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.dfc.DfcCrypto;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.utils.DfcLaunchHelper;
import com.documentum.fc.common.DfException;

public class DctmQueryExecutor {

	private final Logger console = LoggerFactory.getLogger("console");
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DfcLaunchHelper dfcLaunchHelper;

	public DctmQueryExecutor(DfcLaunchHelper dfcLaunchHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
	}

	protected int run(OptionValues cli, Collection<String> positionals) throws Exception {
		// final boolean debug = cli.isPresent(CLIParam.debug);
		final ResultsFormat format = cli.getEnum(ResultsFormat.class, CLIParam.format);
		final File target = Tools.canonicalize(new File(cli.getString(CLIParam.target)));
		final int batchSize = cli.getInteger(CLIParam.batch);
		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String user = this.dfcLaunchHelper.getDfcUser(cli);
		final String password = this.dfcLaunchHelper.getDfcPassword(cli);
		final String dql = positionals.iterator().next();

		final DfcSessionPool pool;
		try {
			pool = new DfcSessionPool(docbase, user, new DfcCrypto().decrypt(password));
		} catch (DfException e) {
			this.log.error("Failed to create the DFC session pool", e);
			return 1;
		}

		int ret = 1;
		try {
			// TODO: Support multiple queries simultaneously?
			final QueryLogic queryLogic = new QueryLogic(this.console, pool, format::newPersistor, dql, target,
				batchSize);
			queryLogic.call();
			ret = 0;
		} finally {
			pool.close();
		}
		return ret;
	}
}