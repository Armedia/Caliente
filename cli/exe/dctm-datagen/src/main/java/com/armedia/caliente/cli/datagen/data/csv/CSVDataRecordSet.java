package com.armedia.caliente.cli.datagen.data.csv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.armedia.caliente.cli.datagen.data.DataRecord;
import com.armedia.caliente.cli.datagen.data.DataRecordSet;
import com.armedia.commons.utilities.Tools;

public final class CSVDataRecordSet extends DataRecordSet<CSVParser, CSVRecord, CSVDataRecordSetInitState> {

	private static final URL getUrl(File f) throws IOException {
		if (f == null) { throw new IllegalArgumentException("Must provide a file to convert to a URL"); }
		f = Tools.canonicalize(f);
		if (!f.exists()) {
			throw new FileNotFoundException(String.format("The file [%s] could not be found", f.getAbsolutePath()));
		}
		if (!f.isFile()) {
			throw new IOException(String.format("The file [%s] is not a regular file", f.getAbsolutePath()));
		}
		if (!f.canRead()) {
			throw new FileNotFoundException(String.format("The file [%s] cannot be read", f.getAbsolutePath()));
		}
		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			// Should never happen...still
			throw new IOException(
				String.format("The file specified by [%s] could not produce a valid URL", f.toString()), e);
		}
	}

	public CSVDataRecordSet(File file) throws Exception {
		this(file, null, null, 1);
	}

	public CSVDataRecordSet(File file, Charset charset) throws Exception {
		this(file, charset, null, 1);
	}

	public CSVDataRecordSet(File file, CSVFormat format) throws Exception {
		this(file, null, format, 1);
	}

	public CSVDataRecordSet(File file, int loopCount) throws Exception {
		this(file, null, null, loopCount);
	}

	public CSVDataRecordSet(File file, Charset charset, CSVFormat format) throws Exception {
		this(file, charset, format, 1);
	}

	public CSVDataRecordSet(File file, Charset charset, int loopCount) throws Exception {
		this(file, charset, null, loopCount);
	}

	public CSVDataRecordSet(File file, CSVFormat format, int loopCount) throws Exception {
		this(file, null, format, loopCount);
	}

	public CSVDataRecordSet(File file, Charset charset, CSVFormat format, int loopCount) throws Exception {
		this(CSVDataRecordSet.getUrl(file), charset, format, loopCount);
	}

	public CSVDataRecordSet(URL url) throws Exception {
		this(url, null, null, 1);
	}

	public CSVDataRecordSet(URL url, Charset charset) throws Exception {
		this(url, charset, null, 1);
	}

	public CSVDataRecordSet(URL url, CSVFormat format) throws Exception {
		this(url, null, format, 1);
	}

	public CSVDataRecordSet(URL url, int loopCount) throws Exception {
		this(url, null, null, loopCount);
	}

	public CSVDataRecordSet(URL url, Charset charset, CSVFormat format) throws Exception {
		this(url, charset, format, 1);
	}

	public CSVDataRecordSet(URL url, Charset charset, int loopCount) throws Exception {
		this(url, charset, null, loopCount);
	}

	public CSVDataRecordSet(URL url, CSVFormat format, int loopCount) throws Exception {
		this(url, null, format, loopCount);
	}

	public CSVDataRecordSet(URL url, Charset charset, CSVFormat format, int loopCount) throws Exception {
		super(true, loopCount, new CSVDataRecordSetInitState(url, charset, format));
	}

	@Override
	protected CSVParser initData() throws Exception {
		CSVParser newParser = CSVParser.parse(this.state.url, this.state.charset, this.state.format);
		// Make sure the headers are read
		Map<String, Integer> headerMap = newParser.getHeaderMap();
		if ((headerMap == null) || headerMap.isEmpty()) {
			throw new Exception(
				String.format("The CSV data at [%s] does not contain a header record", this.state.url.toString()));
		}
		return newParser;
	}

	@Override
	protected Map<String, Integer> mapColumns(CSVParser data) {
		return data.getHeaderMap();
	}

	@Override
	protected Iterator<CSVRecord> getIterator(CSVParser data) {
		return data.iterator();
	}

	@Override
	protected DataRecord newRecord(final CSVRecord r) {
		return new DataRecord() {
			@Override
			public Iterator<String> iterator() {
				return r.iterator();
			}

			@Override
			public String get(int c) {
				return r.get(c);
			}

			@Override
			public String get(String name) {
				return r.get(name);
			}

			@Override
			public <E extends Enum<E>> String get(E e) {
				return r.get(e);
			}

			@Override
			public long getNumber() {
				return r.getRecordNumber();
			}

			@Override
			public boolean hasColumn(String name) {
				return r.isMapped(name);
			}

			@Override
			public boolean hasValue(String name) {
				return r.isSet(name);
			}

			@Override
			public Map<String, String> asMap() {
				return r.toMap();
			}

			@Override
			public boolean isComplete() {
				return r.isConsistent();
			}
		};
	}

	@Override
	protected void closeData(CSVParser data) {
		if (!data.isClosed()) {
			try {
				data.close();
			} catch (IOException e) {
				// Ignore...
			}
		}
	}
}