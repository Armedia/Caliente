package com.armedia.caliente.engine.ucm.model;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.ucm.model.UcmExceptionData.Entry;

public class UcmExceptionParserTest {

	@Test
	public void testParseMessages() {
		String[] msg = {
			"!csUnableToGetRevInfo2,SOME\\!THING\\!\\,WEIRD\\!\\!!csGetFileUnableToFindRevision,csGetFileRevMethodLabel_Latest,SOME\\!THING\\!WE\\,IRD\\!\\!"
		};

		List<Entry> l = UcmExceptionData.parseMessageKey(msg[0]);

		Entry e = l.get(0);
		Assertions.assertEquals("csUnableToGetRevInfo2", e.getTag());
		Assertions.assertEquals(1, e.getParameters().size());
		Assertions.assertEquals("SOME!THING!,WEIRD!!", e.getParameters().get(0));

		e = l.get(1);
		Assertions.assertEquals("csGetFileUnableToFindRevision", e.getTag());
		Assertions.assertEquals(2, e.getParameters().size());
		Assertions.assertEquals("csGetFileRevMethodLabel_Latest", e.getParameters().get(0));
		Assertions.assertEquals("SOME!THING!WE,IRD!!", e.getParameters().get(1));

		Assertions.assertEquals(msg[0], UcmExceptionData.generateMessageKey(l));
	}
}