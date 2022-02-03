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
package com.armedia.caliente.cli.datagen;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.tools.ParameterTools;
import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionParseResult;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.launcher.AbstractEntrypoint;
import com.armedia.commons.utilities.cli.launcher.LaunchClasspathHelper;
import com.armedia.commons.utilities.cli.utils.DfcLaunchHelper;
import com.armedia.commons.utilities.cli.utils.LibLaunchHelper;
import com.armedia.commons.utilities.cli.utils.ThreadsLaunchHelper;

public class Entrypoint extends AbstractEntrypoint {

	protected static final int MIN_THREADS = 1;
	protected static final int DEFAULT_THREADS = (Runtime.getRuntime().availableProcessors() / 2);
	protected static final int MAX_THREADS = (Runtime.getRuntime().availableProcessors());

	private final LibLaunchHelper libLaunchHelper = ParameterTools.CALIENTE_LIB;
	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);
	private final ThreadsLaunchHelper threadsLaunchHelper = new ThreadsLaunchHelper(Entrypoint.MIN_THREADS,
		Entrypoint.DEFAULT_THREADS, Entrypoint.MAX_THREADS);

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		return Arrays.asList(this.libLaunchHelper, this.dfcLaunchHelper);
	}

	@Override
	public String getName() {
		return "caliente-datagen";
	}

	@Override
	protected OptionScheme getOptionScheme() {
		return new OptionScheme(getName()) //
			.addGroup( //
				this.libLaunchHelper.asGroup() //
			) //
			.addGroup( //
				this.dfcLaunchHelper.asGroup() //
			) //
			.addGroup( //
				this.threadsLaunchHelper.asGroup() //
			) //
			.addFrom(Option.unwrap(CLIParam.values())) //
		;
	}

	@Override
	protected int execute(OptionParseResult result) throws Exception {
		return new DataGen(this.threadsLaunchHelper, this.dfcLaunchHelper).run(result.getOptionValues());
	}
}