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
package com.armedia.caliente.cli.bulkdel;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractExecutable;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;

public class Launcher extends AbstractExecutable {

	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);
	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();

	@Override
	protected OptionScheme getOptionScheme() {
		return new OptionScheme(getProgramName()) //
			.addGroup( //
				this.libLaunchHelper.asGroup() //
			) //
			.addGroup( //
				this.dfcLaunchHelper.asGroup() //
			) //
			.addFrom( //
				Option.unwrap(CLIParam.values()) //
			) //
		;
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		return Arrays.asList(this.libLaunchHelper, this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName() {
		return "caliente-bulk-deleter";
	}

	@Override
	protected int execute(OptionValues baseValues, String command, OptionValues commandValies,
		Collection<String> positionals) throws Exception {
		return new BulkDel(this.dfcLaunchHelper).run(baseValues);
	}
}