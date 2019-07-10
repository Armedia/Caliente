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
package com.armedia.caliente.cli.datagen.data.csv;

import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;

import com.armedia.commons.utilities.Tools;

class CSVDataRecordSetInitState {
	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	final URL url;
	final Charset charset;
	final CSVFormat format;

	CSVDataRecordSetInitState(URL url, Charset charset, CSVFormat format) {
		if (url == null) { throw new IllegalArgumentException("Must provide a URL to read from"); }
		this.url = url;
		this.charset = Tools.coalesce(charset, CSVDataRecordSetInitState.DEFAULT_CHARSET);
		format = Tools.coalesce(format, CSVFormat.DEFAULT);
		// Enforce the use of complete header records
		format = format.withAllowMissingColumnNames(false);
		format = format.withFirstRecordAsHeader();
		format = format.withSkipHeaderRecord(false);
		this.format = format;
	}
}