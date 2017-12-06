package com.armedia.caliente.engine.ucm.model;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.ucm.model.UcmExceptionData.Entry;

public class UcmExceptionParserTest {

	@Test
	public void testParseMessages() {
		String[] msg = {
			"!csUnableToGetRevInfo2,SOME\\!THING\\!\\,WEIRD\\!\\!!csGetFileUnableToFindRevision,csGetFileRevMethodLabel_Latest,SOME\\!THING\\!WE\\,IRD\\!\\!"
		};

		List<Entry> l = UcmExceptionData.parseMessageKey(msg[0]);

		Entry e = l.get(0);
		Assert.assertEquals("csUnableToGetRevInfo2", e.getTag());
		Assert.assertEquals(1, e.getParameters().size());
		Assert.assertEquals("SOME!THING!,WEIRD!!", e.getParameters().get(0));

		e = l.get(1);
		Assert.assertEquals("csGetFileUnableToFindRevision", e.getTag());
		Assert.assertEquals(2, e.getParameters().size());
		Assert.assertEquals("csGetFileRevMethodLabel_Latest", e.getParameters().get(0));
		Assert.assertEquals("SOME!THING!WE,IRD!!", e.getParameters().get(1));

		Assert.assertEquals(msg[0], UcmExceptionData.generateMessageKey(l));
	}
}