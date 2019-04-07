package com.armedia.caliente.engine.ucm.exporter;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UcmExportEngineTest {

	@Test
	public void testDecodePathList() {
		String[] paths = {
			"a", "a,b", "a,b,c", "a,b,c,d e,f,g,h"
		};
		List<String> pathList = Arrays.asList(paths);
		String encoded = UcmExportEngine.encodePathList(paths);
		Assertions.assertEquals(pathList, UcmExportEngine.decodePathList(encoded));
	}

	@Test
	public void testEncodePathList() {
		String str = "a,a\\,b,a\\,b\\,c,a\\,b\\,c\\,d e\\,f\\,g\\,h";
		List<String> decoded = UcmExportEngine.decodePathList(str);
		Assertions.assertEquals(str, UcmExportEngine.encodePathList(decoded));
	}

}
