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
package com.armedia.caliente.cli.flat2db;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;

public class Launcher extends AbstractLauncher {
	private static final int MIN_THREADS = 1;
	private static final int DEFAULT_THREADS = Math.min(16, Runtime.getRuntime().availableProcessors() * 2);
	private static final int MAX_THREADS = (Runtime.getRuntime().availableProcessors() * 3);

	private static final String REPORT_MARKER_FORMAT = "yyyyMMdd-HHmmss";

	private final ThreadsLaunchHelper threadsLaunchHelper = new ThreadsLaunchHelper(Launcher.MIN_THREADS,
		Launcher.DEFAULT_THREADS, Launcher.MAX_THREADS);

	@Override
	protected String getProgramName() {
		return "caliente-flat2db";
	}

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	@Override
	protected int run(OptionValues cli, String command, OptionValues commandValues, Collection<String> positionals)
		throws Exception {
		final String reportMarker = DateFormatUtils.format(new Date(), Launcher.REPORT_MARKER_FORMAT);
		System.setProperty("logName", String.format("%s-%s", getProgramName(), reportMarker));
		return 0;
	}

	@Override
	protected OptionScheme getOptionScheme() {
		return new OptionScheme(getProgramName()) //
			.addGroup( //
				this.threadsLaunchHelper.asGroup() //
			) //
			.addFrom( //
				Option.unwrap(CLIParam.values()) //
			) //
			.setArgumentName("configuration-file") //
			.setMinArguments(1) //
			.setMaxArguments(1) //
		;
	}
}