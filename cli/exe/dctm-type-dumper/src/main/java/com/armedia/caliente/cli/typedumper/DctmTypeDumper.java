/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.cli.typedumper;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.tools.dfc.DfcCrypto;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedConsumer;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;

public class DctmTypeDumper {

	private final Logger console = LoggerFactory.getLogger("console");
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DfcLaunchHelper dfcLaunchHelper;
	private final ThreadsLaunchHelper threadHelper;

	public DctmTypeDumper(DfcLaunchHelper dfcLaunchHelper, ThreadsLaunchHelper threadHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
		this.threadHelper = threadHelper;
	}

	private String calculateHierarchy(IDfType t) throws DfException {
		List<String> l = new LinkedList<>();
		while (t != null) {
			l.add(t.getName());
			t = t.getSuperType();
		}
		Collections.reverse(l);
		return FileNameTools.reconstitute(l, false, false);
	}

	protected int run(OptionValues cli) throws Exception {
		// final boolean debug = cli.isPresent(CLIParam.debug);
		final PersistenceFormat format = cli.getEnum(PersistenceFormat.class, CLIParam.format);
		final File target = Tools.canonicalize(new File(cli.getString(CLIParam.target)));
		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String user = this.dfcLaunchHelper.getDfcUser(cli);
		final String password = this.dfcLaunchHelper.getDfcPassword(cli);
		final int threads = this.threadHelper.getThreads(cli);

		final DfcSessionPool pool;
		try {
			pool = new DfcSessionPool(docbase, user, new DfcCrypto().decrypt(password));
		} catch (DfException e) {
			this.log.error("Failed to create the DFC session pool", e);
			return 1;
		}

		int ret = 1;
		try {
			final PooledWorkers<IDfSession, String> extractors = new PooledWorkers<>();
			final ConcurrentMap<String, IDfType> types = new ConcurrentHashMap<>();
			final CheckedConsumer<IDfType, DfException> typeConsumer = (t) -> {
				String hierarchy = calculateHierarchy(t);
				this.console.info("Found {}", hierarchy);
				types.put(hierarchy, t);
			};
			final Predicate<String> typeFilter = (typeName) -> true;

			final ExtractorLogic extractorLogic = new ExtractorLogic(pool //
				, typeConsumer //
				, typeFilter //
			);

			this.console.info("Starting the type search...");
			extractors.start(extractorLogic, Math.max(1, threads), "Extractor", true);
			final IDfSession session = pool.acquireSession();
			final String dql = "select super_name, name from dm_type order by 1, 2";
			try (DfcQuery typeQuery = new DfcQuery(session, dql, DfcQuery.Type.DF_EXECREAD_QUERY)) {
				typeQuery.forEachRemaining((t) -> extractors.addWorkItem(t.getString("name")));
			} finally {
				extractors.waitForCompletion();
				this.console.info("Type search completed, found {} types", types.size());
				pool.releaseSession(session);
			}

			// The key is a hierarchical representation of the type's inheritance, so we
			// can easily order them in the correct sequence
			Set<String> typeOrder = new TreeSet<>(types.keySet());

			try (TypePersistor persistor = format.newPersistor()) {
				persistor.initialize(target);
				for (String type : typeOrder) {
					persistor.persist(types.get(type));
				}
			}
			ret = 0;
		} finally {
			pool.close();
		}
		return ret;
	}
}