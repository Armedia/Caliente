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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Page;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.caliente.tools.CsvFormatter;
import com.armedia.commons.utilities.Tools;

public class CsvContentPersistor extends FileContentPersistor {
	private static final String BASE_NAME = "CSV";
	private static final Collection<Rendition> NULL_RENDITIONS = Tools
		.freezeCollection(Collections.singleton(new Rendition()));
	private static final Collection<Page> NULL_PAGES = Tools
		.freezeCollection(Collections.singleton(new Page().setPath("")));

	private static final CsvFormatter FORMAT = new CsvFormatter(true, //
		"R_OBJECT_ID", //
		"I_CHRONICLE_ID", //
		"R_VERSION_LABEL", //
		"HAS_FOLDER", //
		"DOCUMENTUM_PATH", //
		"FORMAT", //
		"CONTENT_ID", //
		"LENGTH", //
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
	protected void persistContent(Content content) throws Exception {
		if (content == null) { return; }
		final String path;
		List<String> paths = content.getPaths();
		if ((paths == null) || paths.isEmpty()) {
			path = "";
		} else {
			path = paths.get(0);
		}
		// We lock high because we want all the output to be grouped
		// together. This will lead to contention, but the organization
		// of the file is more important
		Collection<Rendition> renditions = content.getRenditions();
		if ((renditions == null) || renditions.isEmpty()) {
			renditions = CsvContentPersistor.NULL_RENDITIONS;
		}
		for (Rendition rendition : renditions) {
			Collection<Page> pages = rendition.getPages();
			if ((pages == null) || pages.isEmpty()) {
				pages = CsvContentPersistor.NULL_PAGES;
			}
			for (Page page : pages) {
				this.out.write(CsvContentPersistor.FORMAT.render( //
					content.getObjectId(), //
					content.getHistoryId(), //
					content.getVersion(), //
					content.isCurrent(), //
					path, //
					rendition.getFormat(), //
					page.getContentId(), //
					page.getLength(), //
					page.getPath() //
				));
				this.out.flush();
			}
		}
	}
}