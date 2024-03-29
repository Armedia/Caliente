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
package com.armedia.caliente.cli.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;

import com.armedia.commons.utilities.LazyFormatter;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionParseResult;
import com.armedia.commons.utilities.cli.OptionScheme;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.launcher.AbstractEntrypoint;
import com.armedia.commons.utilities.cli.utils.ThreadsLaunchHelper;

public class Entrypoint extends AbstractEntrypoint {
	private static final int MIN_THREADS = 1;
	private static final int DEFAULT_THREADS = Math.min(16, Runtime.getRuntime().availableProcessors() * 2);
	private static final int MAX_THREADS = (Runtime.getRuntime().availableProcessors() * 3);

	private static final String REPORT_MARKER_FORMAT = "yyyyMMdd-HHmmss";

	private static boolean verifyPath(Logger log, File f, String label) throws IOException {
		if (!f.exists()) {
			log.error("The {} folder [{}] doesn't exist", label, f.getAbsolutePath());
			return false;
		}
		if (!f.isDirectory()) {
			log.error("The location at [{}] is not a valid {} folder", f.getAbsolutePath(), label);
			return false;
		}
		if (!f.canRead()) {
			log.error("The {} folder [{}] is not readable", label, f.getAbsolutePath());
			return false;
		}
		return true;
	}

	private final ThreadsLaunchHelper threadsLaunchHelper = new ThreadsLaunchHelper(Entrypoint.MIN_THREADS,
		Entrypoint.DEFAULT_THREADS, Entrypoint.MAX_THREADS);

	@Override
	public String getName() {
		return "caliente-validator";
	}

	@Override
	protected int execute(OptionParseResult result) throws Exception {
		final OptionValues cli = result.getOptionValues();
		final String reportMarker = DateFormatUtils.format(new Date(), Entrypoint.REPORT_MARKER_FORMAT);
		System.setProperty("logName", String.format("%s-%s", getName(), reportMarker));

		final File biFile = Tools.canonicalize(new File(cli.getString(CLIParam.bulk_import)));
		if (!Entrypoint.verifyPath(this.log, biFile, "bulk import")) { return 1; }
		final File beFile = Tools.canonicalize(new File(cli.getString(CLIParam.bulk_export)));
		if (!Entrypoint.verifyPath(this.log, beFile, "bulk export")) { return 1; }

		final String reportDirStr = cli.getString(CLIParam.report_dir, System.getProperty("user.dir"));
		File reportDir = Tools.canonicalize(new File(reportDirStr));

		try {
			FileUtils.forceMkdir(reportDir);
		} catch (IOException e) {
			this.log.error("Failed to ensure that the target directory [{}] exists", reportDir.getAbsolutePath());
			return 1;
		}

		final int threads = this.threadsLaunchHelper.getThreads(cli);

		this.log.info("Starting validation with {} thread{}", threads, threads > 1 ? "s" : "");
		Runtime runtime = Runtime.getRuntime();
		this.log.info("Current heap size: {} MB", runtime.totalMemory() / 1024 / 1024);
		this.log.info("Maximum heap size: {} MB", runtime.maxMemory() / 1024 / 1024);
		this.log.info("Bulk Import path: [{}]", biFile.getAbsolutePath());
		this.log.info("Bulk Export path: [{}]", beFile.getAbsolutePath());
		this.log.info("Report directory: [{}]", reportDir.getAbsolutePath());
		this.log.info("Content models  : {}", cli.getStrings(CLIParam.model));
		this.log.info("Report marker   : [{}]", reportMarker);

		final Validator validator = new Validator(reportDir.toPath(), biFile.toPath(), beFile.toPath(),
			cli.getStrings(CLIParam.model), reportMarker);
		final long start = System.currentTimeMillis();
		try {
			final PooledWorkers<Object, Path> workers = new PooledWorkers.Builder<Object, Path, RuntimeException>()
				.logic((o, p) -> validator.validate(p)) //
				.threads(threads) //
				.name("Validator") //
				.waitForWork(true) //
				.start() //
			;

			try {
				Files.walkFileTree(validator.getSourceRoot(), validator.new FileVisitor() {
					@Override
					protected void processFile(Path file) throws Exception {
						workers.addWorkItem(file);
					}
				});
			} finally {
				workers.waitForCompletion();
			}
		} finally {
			long duration = System.currentTimeMillis() - start;
			final long hours = TimeUnit.MILLISECONDS.toHours(duration);
			duration -= TimeUnit.HOURS.toMillis(hours);
			final long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
			duration -= TimeUnit.MINUTES.toMillis(minutes);
			final long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
			duration -= TimeUnit.SECONDS.toMillis(seconds);

			try {
				validator.writeAndClear();
			} finally {
				String format = "Total duration: {}:{}:{}.{}";
				Object[] args = {
					hours, LazyFormatter.of("%02", minutes), LazyFormatter.of("%02", seconds),
					LazyFormatter.of("%03", duration)
				};
				this.log.info(format, args);
				this.console.info(format, args);
			}
		}

		return 0;
	}

	@Override
	protected OptionScheme getOptionScheme() {
		return new OptionScheme(getName()) //
			.addGroup( //
				this.threadsLaunchHelper.asGroup() //
			) //
			.addFrom( //
				Option.unwrap(CLIParam.values()) //
			) //
		;
	}
}