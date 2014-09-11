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

public class CmsExporterTest extends AbstractTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testDoExport() throws Throwable {
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
		CmsExporter exporter = new CmsExporter(10);
		exporter
		.doExport(
			store,
			getSessionManager(),
			"from dm_user union select r_object_id from dm_type union select r_object_id from dm_format union select r_object_id from dm_group union select r_object_id from dm_acl");
		QueryRunner qr = new QueryRunner(getDataSource());
		qr.query(
			"select o.object_type, o.object_number, o.object_id, o.object_label, p.traversed from dctm_object o, dctm_export_plan p where p.object_id = o.object_id order by o.object_type, o.object_number",
			new ResultSetHandler<Integer>() {
				@Override
				public Integer handle(ResultSet rs) throws SQLException {
					int count = 0;
					final String columnFormat = "%-12s\t%-6s\t%-16s\t%s%n";
					System.out.printf(columnFormat, "TYPE", "NUMBER", "ID", "LABEL");
					System.out
						.printf("================================================================================%n");
					while (rs.next()) {
						count++;
						System.out.printf(columnFormat, rs.getString(1), rs.getString(2), rs.getString(3),
							rs.getString(4));
					}
					return count;
				}
			});
	}
}