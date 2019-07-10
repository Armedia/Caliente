/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dfc.importer;

import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.dfc.DctmSessionFactory;
import com.armedia.caliente.engine.dfc.DctmSetting;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.AttributeDeclaration;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.TypeDeclaration;
import com.armedia.caliente.tools.dfc.DfcCrypto;
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
		try (DctmSessionFactory factory = new DctmSessionFactory(cfg, new DfcCrypto())) {
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