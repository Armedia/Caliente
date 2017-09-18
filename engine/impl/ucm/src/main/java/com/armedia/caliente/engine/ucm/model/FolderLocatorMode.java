package com.armedia.caliente.engine.ucm.model;

import java.net.URI;

import oracle.stellent.ridc.model.DataBinder;

enum FolderLocatorMode {
	//
	BY_PATH {
		@Override
		protected void applySearchParameters(DataBinder binder, Object key) {
			binder.putLocal("path", key.toString());
		}

		@Override
		protected Object sanitizeKey(Object key) {
			return UcmModel.sanitizePath(key != null ? key.toString() : null);
		}
	},
	BY_GUID {
		@Override
		protected void applySearchParameters(DataBinder binder, Object key) {
			BY_URI.applySearchParameters(binder, UcmUniqueURI.class.cast(key).getURI());
		}
	},
	BY_URI {
		@Override
		protected void applySearchParameters(DataBinder binder, Object key) {
			URI uri = URI.class.cast(key);
			binder.putLocal("fFolderGUID", uri.getSchemeSpecificPart());
		}
	},
	//
	;

	protected abstract void applySearchParameters(DataBinder binder, Object key);

	protected Object sanitizeKey(Object key) {
		return key;
	}
}