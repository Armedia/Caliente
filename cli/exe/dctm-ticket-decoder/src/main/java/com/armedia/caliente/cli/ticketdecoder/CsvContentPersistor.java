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
import java.io.IOException;
import java.util.Objects;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Page;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.caliente.tools.CsvFormatter;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;

public class CsvContentPersistor extends FileContentPersistor {
	private static final String BASE_NAME = "CSV";
	private static final Rendition NULL_RENDITION = new Rendition();
	private static final Page NULL_PAGE = new Page().setPath("");

	private static final CsvFormatter FORMAT = new CsvFormatter(true, //
		"R_OBJECT_ID", //
		"I_CHRONICLE_ID", //
		"R_VERSION_LABEL", //
		"HAS_FOLDER", //
		"DOCUMENTUM_PATH", //
		"CONTENT_ID", //
		"LENGTH", //
		"FORMAT", //
		"CONTENT_STORE_PATH" //
	);

	public CsvContentPersistor(File target) {
		super(Objects.requireNonNull(target), CsvContentPersistor.BASE_NAME);
	}

	@Override
	protected void startup() throws Exception {
		super.startup();
		this.out.write(CsvContentPersistor.FORMAT.renderHeaders());
		this.out.flush();
	}

	@Override
	protected void persistContent(Content content) {
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
			this.out.write(CsvContentPersistor.FORMAT.render( //
				content.getId(), //
				content.getHistoryId(), //
				content.getVersion(), //
				content.isCurrent(), //
				path, //
				page.getContentId(), //
				page.getLength(), //
				rendition.getFormat(), //
				page.getPath() //
			));
			this.out.flush();
		} catch (IOException e) {
			this.error.error("Failed to persist the content object {}", content, e);
		}
	}
}