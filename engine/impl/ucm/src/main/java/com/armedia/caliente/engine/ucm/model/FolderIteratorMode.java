package com.armedia.caliente.engine.ucm.model;

import oracle.stellent.ridc.model.DataBinder;

public enum FolderIteratorMode {
	//
	COMBINED {
		@Override
		protected void setParameters(DataBinder requestBinder) {
			super.setParameters(requestBinder);
			requestBinder.putLocal("doCombinedBrowse", "1");
			requestBinder.putLocal("foldersFirst", "1");
		}
	}, //
	FILES, //
	FOLDERS, //
	//
	;

	final String count;
	final String startRow;

	private FolderIteratorMode() {
		String countLabel = name().toLowerCase();
		this.count = String.format("%sCount", countLabel);
		this.startRow = String.format("%sStartRow", countLabel);
	}

	protected void setParameters(DataBinder requestBinder) {
		requestBinder.putLocal("doRetrieveTargetInfo", "1");
	}
}