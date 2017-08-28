package com.armedia.caliente.cli.help;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;

import com.armedia.caliente.cli.exception.HelpRequestedException;

public abstract class HelpRenderer {
	public static final int DEFAULT_WIDTH = 80;

	public final void renderHelp(String programName, HelpRequestedException help, OutputStream out) throws IOException {
		renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH, out);
	}

	public final void renderHelp(String programName, HelpRequestedException help, int width, OutputStream out)
		throws IOException {
		renderHelp(programName, help, width, out, null);
	}

	public final void renderHelp(String programName, HelpRequestedException help, OutputStream out, Charset encoding)
		throws IOException {
		renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH, out, encoding);
	}

	public final void renderHelp(String programName, HelpRequestedException help, int width, OutputStream out,
		Charset encoding) throws IOException {
		Objects.requireNonNull(out, "Must provide an output stream to write to");
		if (encoding == null) {
			encoding = Charset.defaultCharset();
		}
		renderHelp(programName, help, width, new OutputStreamWriter(out, encoding));
	}

	public final String renderHelp(String programName, HelpRequestedException help) {
		return renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH);
	}

	public final String renderHelp(String programName, HelpRequestedException help, int width) {
		StringWriter w = new StringWriter();
		try {
			renderHelp(programName, help, width, w);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException writing to memory", e);
		}
		return w.toString();
	}

	public final void renderHelp(String programName, HelpRequestedException help, Writer w) throws IOException {
		renderHelp(programName, help, HelpRenderer.DEFAULT_WIDTH, w);
	}

	public final void renderHelp(String programName, HelpRequestedException help, int width, Writer w)
		throws IOException {
		Objects.requireNonNull(programName, "Must provide a program name");
		Objects.requireNonNull(help, "Must provide a scheme to render help for");
		Objects.requireNonNull(w, "Must provide a writer to render on");
	}

	protected abstract void doRenderHelp(String programName, HelpRequestedException help, int width, Writer w)
		throws IOException;
}