import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.cmis.CmisSessionSetting;
import com.armedia.cmf.engine.cmis.CmisSetting;
import com.armedia.cmf.engine.cmis.exporter.CmisExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.Stores;

public class BaseTest {

	@Test
	public void test() throws Exception {
		final ExportEngine<?, ?, ?, ?> engine = CmisExportEngine.getExportEngine();
		ObjectStore<?, ?> objectStore = Stores.getObjectStore("default");
		objectStore.clearProperties();
		objectStore.clearAllObjects();
		objectStore.clearAttributeMappings();
		ContentStore contentStore = Stores.getContentStore("default");
		contentStore.clearProperties();
		contentStore.clearAllStreams();
		Logger output = LoggerFactory.getLogger("console");

		Map<String, String> settings = new TreeMap<String, String>();
		settings.put(CmisSessionSetting.ATOMPUB_URL.getLabel(),
			"http://armedia-vm.rivera.prv/alfresco/api/-default-/public/cmis/versions/1.0/atom");
		settings.put(CmisSessionSetting.USER.getLabel(), "admin");
		settings.put(CmisSessionSetting.PASSWORD.getLabel(), "123");
		settings.put(CmisSessionSetting.REPOSITORY_ID.getLabel(), "-default-");
		// settings.put(CmisSetting.EXPORT_QUERY.getLabel(), "SELECT * FROM cmis:document");
		settings.put(CmisSetting.EXPORT_PATH.getLabel(), "/Shared");
		settings.put(CmisSetting.EXPORT_PAGE_SIZE.getLabel(), "5");
		engine.runExport(output, objectStore, contentStore, settings);
	}
}