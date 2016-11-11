package com.armedia.caliente.cli.datagen.data.csv;

import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;

import com.armedia.commons.utilities.Tools;

class CSVDataRecordSetInitState {
	final URL url;
	final Charset charset;
	final CSVFormat format;

	CSVDataRecordSetInitState(URL url, Charset charset, CSVFormat format) {
		if (url == null) { throw new IllegalArgumentException("Must provide a URL to read from"); }
		this.url = url;
		this.charset = Tools.coalesce(charset, Charset.defaultCharset());
		format = Tools.coalesce(format, CSVFormat.DEFAULT);
		// Enforce the use of complete header records
		format = format.withAllowMissingColumnNames(false);
		format = format.withFirstRecordAsHeader();
		format = format.withSkipHeaderRecord(false);
		this.format = format;
	}
}