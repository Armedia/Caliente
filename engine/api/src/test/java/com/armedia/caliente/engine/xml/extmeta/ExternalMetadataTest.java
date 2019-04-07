package com.armedia.caliente.engine.xml.extmeta;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class ExternalMetadataTest {

	private ExternalMetadataLoader getEMDL(String cfg) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL url = cl.getResource("caliente.mv.db");
		if (url == null) { throw new Exception("No database to test against"); }
		String dbPath = url.getPath();
		dbPath = dbPath.replaceAll("\\.mv\\.db$", "");
		System.setProperty("h2.test.path", dbPath);
		return ExternalMetadataLoader.getExternalMetadataLoader(cfg, true);
	}

	@Test
	public void testSQL() throws Exception {
		ExternalMetadataLoader loader = getEMDL("resource:external-metadata-testsql.xml");
		loader.initialize();
		CmfAttributeTranslator<CmfValue> translator = CmfAttributeTranslator.CMFVALUE_TRANSLATOR;
		Set<String> secondaries = Collections.emptySet();
		List<CmfObjectRef> parentIds = Collections.emptyList();
		CmfObject<CmfValue> obj = new CmfObject<>( //
			translator, //
			CmfObject.Archetype.DOCUMENT, //
			"09de75d1800205d6", //
			"/CMSMFTests/FILES/file4.txt", //
			parentIds, //
			0, //
			"09de75d1800205d6", //
			false, //
			"09de75d1800205d6", //
			"subtype", //
			secondaries, //
			null //
		);

		Object[][] data = {
			{
				"cmf:acl_name", 1, new String[] {
					"dm_45de75d180008d12",
				}
			}, //
			{
				"cmf:group", 1, new String[] {
					"admingroup",
				}
			}, //
			{
				"cmf:group_permission", 1, new String[] {
					"5",
				}
			}, //
			{
				"cmf:last_access_date", 1, new String[] {
					"2019-03-23T18:56:39.000-06:00",
				}
			}, //
			{
				"cmf:login_realm", 1, new String[] {
					"dmadmin2",
				}
			}, //
			{
				"cmf:owner", 1, new String[] {
					"dmadmin2",
				}
			}, //
			{
				"cmf:owner_permission", 1, new String[] {
					"7",
				}
			}, //
			{
				"cmf:version_antecedent_id", 1, new String[] {
					"%ID{0000000000000000}%",
				}
			}, //
			{
				"cmis:changeToken", 1, new String[] {
					"0",
				}
			}, //
			{
				"cmis:checkinComment", 1, new String[] {
					"",
				}
			}, //
			{
				"cmis:contentStreamLength", 1, new String[] {
					"4669641276327460864",
				}
			}, //
			{
				"cmis:contentStreamMimeType", 1, new String[] {
					"text",
				}
			}, //
			{
				"cmis:createdBy", 1, new String[] {
					"dmadmin2",
				}
			}, //
			{
				"cmis:creationDate", 1, new String[] {
					"2014-09-08T12:59:05.000-06:00",
				}
			}, //
			{
				"cmis:description", 1, new String[] {
					"",
				}
			}, //
			{
				"cmis:isImmutable", 1, new String[] {
					"false",
				}
			}, //
			{
				"cmis:isLatestVersion", 1, new String[] {
					"true",
				}
			}, //
			{
				"cmis:lastModificationDate", 1, new String[] {
					"2014-09-08T12:59:05.000-06:00",
				}
			}, //
			{
				"cmis:lastModifiedBy", 1, new String[] {
					"dmadmin2",
				}
			}, //
			{
				"cmis:name", 1, new String[] {
					"file4.txt",
				}
			}, //
			{
				"cmis:objectTypeId", 1, new String[] {
					"dm_document",
				}
			}, //
			{
				"cmis:parentId", 1, new String[] {
					"%ID{0bde75d1800205c8}%",
				}
			}, //
			{
				"cmis:versionLabel", 2, new String[] {
					"1.0", "CURRENT",
				}
			}, //
			{
				"cmis:versionSeriesCheckedOutBy", 1, new String[] {
					"",
				}
			}, //
			{
				"cmis:versionSeriesId", 1, new String[] {
					"%ID{09de75d1800205d6}%",
				}
			}, //
			{
				"dctm:a_application_type", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:a_archive", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:a_category", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:a_compound_architecture", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:a_controlling_app", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:a_full_text", 1, new String[] {
					"true",
				}
			}, //
			{
				"dctm:a_is_hidden", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:a_is_signed", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:a_is_template", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:a_last_review_date", 1, new String[] {
					"{NULL-VALUE}",
				}
			}, //
			{
				"dctm:a_link_resolved", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:a_retention_date", 1, new String[] {
					"{NULL-VALUE}",
				}
			}, //
			{
				"dctm:a_special_app", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:a_status", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:a_storage_type", 1, new String[] {
					"filestore_01",
				}
			}, //
			{
				"dctm:i_branch_cnt", 1, new String[] {
					"0",
				}
			}, //
			{
				"dctm:i_cabinet_id", 1, new String[] {
					"%ID{0cde75d1800205be}%",
				}
			}, //
			{
				"dctm:i_contents_id", 1, new String[] {
					"%ID{06de75d1800279b7}%",
				}
			}, //
			{
				"dctm:i_direct_dsc", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:i_is_deleted", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:i_is_reference", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:i_is_replica", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:i_latest_flag", 1, new String[] {
					"true",
				}
			}, //
			{
				"dctm:i_partition", 1, new String[] {
					"0",
				}
			}, //
			{
				"dctm:i_reference_cnt", 1, new String[] {
					"1",
				}
			}, //
			{
				"dctm:i_retain_until", 1, new String[] {
					"{NULL-VALUE}",
				}
			}, //
			{
				"dctm:language_code", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:r_alias_set_id", 1, new String[] {
					"%ID{0000000000000000}%",
				}
			}, //
			{
				"dctm:r_assembled_from_id", 1, new String[] {
					"%ID{0000000000000000}%",
				}
			}, //
			{
				"dctm:r_content_size", 1, new String[] {
					"15308",
				}
			}, //
			{
				"dctm:r_current_state", 1, new String[] {
					"0",
				}
			}, //
			{
				"dctm:resolution_label", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:r_frozen_flag", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:r_frzn_assembly_cnt", 1, new String[] {
					"0",
				}
			}, //
			{
				"dctm:r_has_events", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:r_has_frzn_assembly", 1, new String[] {
					"false",
				}
			}, //
			{
				"dctm:r_is_public", 1, new String[] {
					"true",
				}
			}, //
			{
				"dctm:r_is_virtual_doc", 1, new String[] {
					"0",
				}
			}, //
			{
				"dctm:r_link_cnt", 1, new String[] {
					"0",
				}
			}, //
			{
				"dctm:r_link_high_cnt", 1, new String[] {
					"0",
				}
			}, //
			{
				"dctm:r_lock_date", 1, new String[] {
					"{NULL-VALUE}",
				}
			}, //
			{
				"dctm:r_lock_machine", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:r_page_cnt", 1, new String[] {
					"1",
				}
			}, //
			{
				"dctm:r_policy_id", 1, new String[] {
					"%ID{0000000000000000}%",
				}
			}, //
			{
				"dctm:r_resume_state", 1, new String[] {
					"0",
				}
			}, //
			{
				"dctm:subject", 1, new String[] {
					"",
				}
			}, //
			{
				"dctm:world_permit", 1, new String[] {
					"3",
				}
			}, //
		};
		Map<String, CmfAttribute<CmfValue>> attributes = loader.getAttributeValues(obj);
		Assertions.assertNotNull(attributes);
		Assertions.assertFalse(attributes.isEmpty());
		for (Object[] d : data) {
			Assertions.assertEquals(3, d.length);
			final String name = Tools.toString(d[0]);
			Assertions.assertNotNull(name);
			final Integer count = Tools.decodeInteger(d[1]);
			Assertions.assertNotNull(count);
			final String[] values = (String[]) d[2];
			Assertions.assertNotNull(values);
			Assertions.assertEquals(count.intValue(), values.length);
			Assertions.assertTrue(attributes.containsKey(name));
			CmfAttribute<CmfValue> att = attributes.get(name);
			Assertions.assertNotNull(att);
			Assertions.assertTrue(att.hasValues());

			int actualCount = att.getValueCount();
			List<CmfValue> actualValues = att.getValues();
			Assertions.assertEquals(count.intValue(), actualCount);
			Assertions.assertEquals(actualCount, actualValues.size());

			for (int i = 0; i < count; i++) {
				Assertions.assertEquals(values[i], att.getValue(i).asString());
			}
		}
	}

	@Test
	public void testDDL() throws Exception {
		ExternalMetadataLoader loader = getEMDL("resource:external-metadata-testddl.xml");
		loader.initialize();
		CmfAttributeTranslator<CmfValue> translator = CmfAttributeTranslator.CMFVALUE_TRANSLATOR;
		Set<String> secondaries = Collections.emptySet();
		List<CmfObjectRef> parentIds = Collections.emptyList();
		CmfObject<CmfValue> obj = new CmfObject<>( //
			translator, //
			CmfObject.Archetype.DOCUMENT, //
			"09de75d180020638", //
			"/CMSMFTests/LANDING/ACCESS/READ_NONE/primary.png", //
			parentIds, //
			0, //
			"09de75d180020638", //
			false, //
			"09de75d180020638", //
			"subtype", //
			secondaries, //
			352L //
		);

		Object[][] data = {
			{
				"cmf:HISTORY_CURRENT", 1, CmfValue.Type.BOOLEAN, new Object[] {
					true,
				}
			}, //
			{
				"cmf:HISTORY_ID", 1, CmfValue.Type.STRING, new Object[] {
					"09de75d180020638",
				}
			}, //
			{
				"cmf:OBJECT_LABEL", 1, CmfValue.Type.STRING, new Object[] {
					"/CMSMFTests/LANDING/ACCESS/READ_NONE/primary.png#1.0,CURRENT",
				}
			}, //
			{
				"cmf:OBJECT_NAME", 1, CmfValue.Type.STRING, new Object[] {
					"primary.png",
				}
			}, //
			{
				"cmf:OBJECT_SUBTYPE", 1, CmfValue.Type.STRING, new Object[] {
					"dm_document",
				}
			}, //
			{
				"cmf:OBJECT_TYPE", 1, CmfValue.Type.STRING, new Object[] {
					"DOCUMENT",
				}
			}, //
			{
				"cmf:someSearchKey", 1, CmfValue.Type.STRING, new Object[] {
					"09de75d180020638",
				}
			}, //
			{
				"cmis:objectId", 1, CmfValue.Type.STRING, new Object[] {
					"{07-09de75d180020638}",
				}
			}, //
			{
				"cmf:objectIndexWithinExport", 1, CmfValue.Type.INTEGER, new Object[] {
					352L,
				}
			}, //
		};
		Map<String, CmfAttribute<CmfValue>> attributes = loader.getAttributeValues(obj);
		Assertions.assertNotNull(attributes);
		Assertions.assertFalse(attributes.isEmpty());
		for (Object[] d : data) {
			Assertions.assertEquals(4, d.length);
			final String name = Tools.toString(d[0]);
			Assertions.assertNotNull(name);
			final Integer count = Tools.decodeInteger(d[1]);
			Assertions.assertNotNull(count);
			final CmfValue.Type type = CmfValue.Type.class.cast(d[2]);
			Assertions.assertNotNull(type);
			final Object[] values = (Object[]) d[3];
			Assertions.assertNotNull(values);
			Assertions.assertEquals(count.intValue(), values.length);
			Assertions.assertTrue(attributes.containsKey(name), String.format("Does not contain the key [%s]", name));
			CmfAttribute<CmfValue> att = attributes.get(name);
			Assertions.assertNotNull(att);
			Assertions.assertTrue(att.hasValues());
			Assertions.assertEquals(name, att.getName());
			Assertions.assertSame(type, att.getType());

			int actualCount = att.getValueCount();
			List<CmfValue> actualValues = att.getValues();
			Assertions.assertEquals(count.intValue(), actualCount);
			Assertions.assertEquals(actualCount, actualValues.size());

			for (int i = 0; i < count; i++) {
				Assertions.assertEquals(values[i], att.getValue(i).asObject());
			}
		}
	}
}