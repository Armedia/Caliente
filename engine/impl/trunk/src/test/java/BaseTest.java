import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.cmis.CmisSessionSetting;
import com.armedia.cmf.engine.cmis.exporter.CmisExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.Stores;

public class BaseTest {

	@Test
	public void test() throws Exception {
		ExportEngine<?, ?, ?, ?, ?> e = CmisExportEngine.getExportEngine();
		ObjectStore<?, ?> db = Stores.getObjectStore("default");
		ContentStore content = Stores.getContentStore("default");

		Map<String, String> settings = new TreeMap<String, String>();
		Logger out = LoggerFactory.getLogger("console");

		settings.put(CmisSessionSetting.BASE_URL.getLabel(), "http://armedia-vm.rivera.prv/alfresco");
		settings.put(CmisSessionSetting.USER.getLabel(), "admin");
		settings.put(CmisSessionSetting.PASSWORD.getLabel(), "123");

		e.runExport(out, db, content, settings);
	}
}