package com.delta.cmsmf.engine;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.delta.cmsmf.cms.AbstractTest;
import com.delta.cmsmf.cms.storage.CmsObjectStore;

public class CmsImporterTest extends AbstractTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testDoImport() throws Throwable {
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
		CmsExporter exporter = new CmsExporter(10, 10);
		CmsImporter importer = new CmsImporter(10, 10);

		// big crap - includes "everything":
		// "from dm_user union select r_object_id from dm_type union select r_object_id from dm_format union select r_object_id from dm_group union select r_object_id from dm_acl union select r_object_id from dm_sysobject where folder('/CMSMFTests', DESCEND)"
		exporter.doExport(store, getSourceSessionManager(), "from dm_sysobject where folder('/CMSMFTests', DESCEND)");
		QueryRunner qr = new QueryRunner(getDataSource());
		qr.query(
			"select o.object_type, o.object_subtype, o.object_number, o.object_id, o.object_label from dctm_object o, dctm_export_plan p where p.object_id = o.object_id order by o.object_type, o.object_number",
			new ResultSetHandler<Integer>() {
				@Override
				public Integer handle(ResultSet rs) throws SQLException {
					int count = 0;
					final String columnFormat = "%-12s\t%-12s\t%-6s\t%-16s\t%s%n";
					System.out.printf(columnFormat, "TYPE", "SUBTYPE", "NUMBER", "ID", "LABEL");
					System.out
					.printf("==========================================================================================%n");
					while (rs.next()) {
						count++;
						System.out.printf(columnFormat, rs.getString(1), rs.getString(2), rs.getString(3),
							rs.getString(4), rs.getString(5));
					}
					return count;
				}
			});
		importer.doImport(store, getTargetSessionManager());
	}
}