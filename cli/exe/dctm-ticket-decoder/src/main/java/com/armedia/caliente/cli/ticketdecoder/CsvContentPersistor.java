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
package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.commons.text.StringEscapeUtils;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Page;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;

public class CsvContentPersistor extends BaseShareableLockable implements ContentPersistor {

	private PrintWriter out = null;

	private static final Rendition NULL_RENDITION = new Rendition();
	private static final Page NULL_PAGE = new Page().setPath("");

	@Override
	public void initialize(final File target) throws Exception {
		final File finalTarget = Tools.canonicalize(target);
		try (MutexAutoLock lock = autoMutexLock()) {
			this.out = new PrintWriter(new FileWriter(finalTarget));
			this.out.printf("R_OBJECT_ID,DOCUMENTUM_PATH,LENGTH,FORMAT,CONTENT_STORE_PATH%n");
			this.out.flush();
		}
	}

	@Override
	public void persist(Content content) throws Exception {
		if (content == null) { return; }
		final Rendition rendition;
		if (!content.getRenditions().isEmpty()) {
			rendition = content.getRenditions().get(0);
		} else {
			rendition = CsvContentPersistor.NULL_RENDITION;
		}
		final Page page;
		if (!rendition.getPages().isEmpty()) {
			page = rendition.getPages().get(0);
		} else {
			page = CsvContentPersistor.NULL_PAGE;
		}
		final String path;
		if (!content.getPaths().isEmpty()) {
			path = content.getPaths().get(0);
		} else {
			path = "";
		}
		try (MutexAutoLock lock = autoMutexLock()) {
			this.out.printf("%s,%s,%d,%s,%s%n", //
				StringEscapeUtils.escapeCsv(content.getId()), //
				StringEscapeUtils.escapeCsv(path), //
				page.getLength(), //
				StringEscapeUtils.escapeCsv(rendition.getFormat()), //
				StringEscapeUtils.escapeCsv(page.getPath()) //
			);
		}
	}

	@Override
	public void close() throws Exception {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.out.flush();
			this.out.close();
		}
	}
}