package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.commons.text.StringEscapeUtils;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseReadWriteLockable;

public class CsvContentPersistor extends BaseReadWriteLockable implements ContentPersistor {

	private PrintWriter out = null;

	private static final Rendition NULL_RENDITION = new Rendition().setLength(0).setPath("").setFormat("").setNumber(0);

	@Override
	public void initialize(final File target) throws Exception {
		final File finalTarget = Tools.canonicalize(target);
		writeLocked(() -> {
			this.out = new PrintWriter(new FileWriter(finalTarget));
			this.out.printf("R_OBJECT_ID,DOCUMENTUM_PATH,LENGTH,FORMAT,CONTENT_STORE_PATH%n");
			this.out.flush();
		});
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
		writeLocked(() -> {
			this.out.printf("%s,%s,%d,%s,%s%n", //
				StringEscapeUtils.escapeCsv(content.getId()), //
				StringEscapeUtils.escapeCsv(content.getPath()), //
				rendition.getLength(), //
				StringEscapeUtils.escapeCsv(rendition.getFormat()), //
				StringEscapeUtils.escapeCsv(rendition.getPath()) //
			);
		});
	}

	@Override
	public void close() throws Exception {
		writeLocked(() -> {
			this.out.flush();
			this.out.close();
		});
	}
}