package com.armedia.caliente.engine.ucm.model;

import org.junit.Test;

public class UcmExceptionParserTest {

	@Test
	public void testParseMessages() {
		String[] msg = {
			"!csUnableToGetRevInfo2,SOME\\!THING\\!\\,WEIRD\\!\\!!csGetFileUnableToFindRevision,csGetFileRevMethodLabel_Latest,SOME\\!THING\\!WE\\,IRD\\!\\!"
		};

		UcmExceptionData.parseMessages(msg[0]);
	}

}
