package com.armedia.caliente.engine.xml.extmeta;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.armedia.caliente.engine.extmeta.ExternalMetadataLoader;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;

public class ExternalMetadataTest {

	private ExternalMetadataLoader getEMDL(String cfg) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL url = cl.getResource("caliente.mv.db");
		if (url == null) { throw new Exception("No database to test against"); }
		String dbPath = url.getPath();
		dbPath = dbPath.replaceAll("\\.mv\\.db$", "");
		System.setProperty("h2.test.path", dbPath);
		return new ExternalMetadataLoader(cfg);
	}

	/*
	{07-0800144c80028f75}
	{07-0800144c80028f76}
	{07-0800144c80028f7b}
	{07-0800144c8002aba2}
	{07-0800144c8002abc8}
	{07-0800144c8002abe3}
	{07-0800144c8002ac02}
	{07-0800144c8002ac08}
	{07-0800144c8002ac0e}
	{07-0800144c8002ac14}
	{07-0800144c8002ac1d}
	{07-0800144c8002ac23}
	 */

	@Test
	public void testSQL() throws Exception {
		ExternalMetadataLoader loader = getEMDL("resource:external-metadata-testsql.xml");
		loader.initialize();
		CmfAttributeTranslator<CmfValue> translator = CmfAttributeTranslator.CMFVALUE_TRANSLATOR;
		Set<String> secondaries = Collections.emptySet();
		List<CmfObjectRef> parentIds = Collections.emptyList();
		CmfObject<CmfValue> obj = new CmfObject<>( //
			translator, //
			CmfType.DOCUMENT, //
			"0800144c80028f75", //
			"ID Factory", //
			parentIds, //
			0, //
			"0800144c80028f75", //
			false, //
			"0800144c80028f75", //
			"subtype", //
			secondaries, //
			"test", //
			"Test2", //
			null //
		);
		loader.getAttributeValues(obj);
		loader.hashCode();
	}
}