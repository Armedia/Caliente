package com.armedia.caliente.engine.dfc.importer;

import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.Test;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.dfc.DctmSessionFactory;
import com.armedia.caliente.engine.dfc.DctmSetting;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.AttributeDeclaration;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.TypeDeclaration;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;

public class DctmSchemaServiceTest {

	@Test
	public void testSchemaService() throws Exception {
		Map<String, Object> settings = new HashedMap<>();
		/*
		settings.put(DctmSetting.DOCBASE.getLabel(), "documentum");
		settings.put(DctmSetting.USERNAME.getLabel(), "dmadmin2");
		settings.put(DctmSetting.PASSWORD.getLabel(), "XZ6ZkrcrHEg=");
		*/
		settings.put(DctmSetting.DOCBASE.getLabel(), "dctmvm01");
		settings.put(DctmSetting.USERNAME.getLabel(), "dctmadmin");
		settings.put(DctmSetting.PASSWORD.getLabel(), "123");

		CfgTools cfg = new CfgTools(settings);
		try (DctmSessionFactory factory = new DctmSessionFactory(cfg, new DctmCrypto())) {
			try (SessionWrapper<IDfSession> session = factory.acquireSession()) {
				try (DctmSchemaService schema = new DctmSchemaService(session.get())) {
					for (String name : schema.getObjectTypeNames()) {
						TypeDeclaration declaration = schema.getObjectTypeDeclaration(name);
						System.out.printf("OBJECT TYPE    :%s%n", declaration);

						for (AttributeDeclaration attribute : declaration.getAttributes()) {
							System.out.printf("\t%s%n", attribute);
						}
					}
					for (String name : schema.getSecondaryTypeNames()) {
						TypeDeclaration declaration = schema.getSecondaryTypeDeclaration(name);
						System.out.printf("SECONDARY TYPE : %s%n", declaration);

						for (AttributeDeclaration attribute : declaration.getAttributes()) {
							System.out.printf("\t%s%n", attribute);
						}
					}
				}
			}
		}
	}
}