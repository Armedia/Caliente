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
package com.armedia.caliente.cli.history;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.launcher.AbstractEntrypoint;
import com.armedia.commons.utilities.cli.launcher.LaunchClasspathHelper;
import com.armedia.commons.utilities.cli.utils.DfcLaunchHelper;
import com.armedia.commons.utilities.cli.utils.LibLaunchHelper;
import com.armedia.commons.utilities.cli.utils.ThreadsLaunchHelper;

public class Entrypoint extends AbstractEntrypoint {

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();
	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);
	private final ThreadsLaunchHelper threadsLaunchHelper = new ThreadsLaunchHelper();

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
			.addFrom( //
				Option.unwrap(CLIParam.values()) //
			) //
			.setMinArguments(1) //
		;
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		return Arrays.asList(this.libLaunchHelper, this.dfcLaunchHelper);
	}

	@Override
	public String getName() {
		return "caliente-history";
	}

	@Override
	protected int execute(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {
		return new History(this.dfcLaunchHelper, this.threadsLaunchHelper).run(baseValues, positionals);
	}
}