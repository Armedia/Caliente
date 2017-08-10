package com.armedia.caliente.cli.parser.token;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import com.armedia.commons.utilities.Tools;

public class HelpGenerator {

	private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

	public static class Format {
		public String title = null;
		public String header = null;
		public String footer = null;
		public String error = null;
		public int width = 0;
	}

	public static String generateHelp(ParameterSchema schema) {
		return HelpGenerator.generateHelp(schema, null);
	}

	public static String generateHelp(ParameterSchema schema, Format format) {
		final StringWriter w = new StringWriter();
		try {
			HelpGenerator.generateHelp(schema, format, w);
			return w.toString();
		} catch (IOException e) {
			// This should be impossible...
			throw new RuntimeException("Unexpected IOException while writing to memory", e);
		}
	}

	public static void generateHelp(ParameterSchema schema, Format format, OutputStream out, Charset encoding)
		throws IOException {
		HelpGenerator.generateHelp(schema, format,
			new OutputStreamWriter(out, Tools.coalesce(encoding, HelpGenerator.DEFAULT_CHARSET)));
	}

	public static void generateHelp(ParameterSchema schema, Format format, Writer out) throws IOException {
		// Generate the stuff...
	}
}