package com.armedia.caliente.cli.datagen.data.csv;

import java.net.URL;

import com.armedia.caliente.cli.datagen.data.DataRecordManager;
import com.armedia.commons.utilities.Tools;

public abstract class CSVDataRecordManager extends DataRecordManager<URL> {

	@Override
	protected CSVDataRecordSet buildRecordSet(URL url, int loopCount) throws Exception {
		return new CSVDataRecordSet(url, loopCount);
	}

	@Override
	protected String describeLocation(URL location) {
		return Tools.toString(location);
	}
}