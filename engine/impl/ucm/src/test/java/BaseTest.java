import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.ucm.exporter.UcmExportEngine;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStores;

public class BaseTest {

	@Test
	public void test() throws Exception {
		final ExportEngine<?, ?, ?, ?, ?, ?> engine = UcmExportEngine.getExportEngine();
		Logger output = LoggerFactory.getLogger("console");

		Map<String, String> settings = new TreeMap<>();
		/*
		settings.put(UcmSessionSetting.ATOMPUB_URL.getLabel(),
			"http://armedia-vm.rivera.prv/alfresco/api/-default-/public/cmis/versions/1.0/atom");
		settings.put(UcmSessionSetting.USER.getLabel(), "admin");
		settings.put(UcmSessionSetting.PASSWORD.getLabel(), "123");
		settings.put(UcmSessionSetting.REPOSITORY_ID.getLabel(), "-default-");
		// settings.put(CmisSetting.EXPORT_QUERY.getLabel(), "SELECT * FROM cmis:document");
		settings.put(CmisSetting.EXPORT_PATH.getLabel(), "/Shared");
		settings.put(CmisSetting.EXPORT_PAGE_SIZE.getLabel(), "5");
		*/

		CmfObjectStore<?, ?> objectStore = CmfStores.getObjectStore("default");
		objectStore.clearProperties();
		objectStore.clearAllObjects();
		objectStore.clearAttributeMappings();
		CmfContentStore<?, ?, ?> contentStore = CmfStores.getContentStore("default");
		contentStore.clearProperties();
		contentStore.clearAllStreams();
		engine.runExport(output, null, objectStore, contentStore, settings);
	}
}