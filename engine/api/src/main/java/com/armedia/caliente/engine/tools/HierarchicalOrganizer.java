package com.armedia.caliente.engine.tools;

import java.util.ArrayList;
import java.util.List;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

public class HierarchicalOrganizer extends LocalOrganizer {
	public static final String NAME = "hierarchical";

	public HierarchicalOrganizer() {
		this(HierarchicalOrganizer.NAME);
	}

	protected HierarchicalOrganizer(String name) {
		super(name);
	}

	@Override
	protected <T> Location calculateLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {

		int folderLevels = 3;
		// A maximum of 7 levels...
		folderLevels = Tools.ensureBetween(3, folderLevels, 7);

		List<String> paths = new ArrayList<>();
		// Make sure the contents all land in the bulk-import root location, so it's easy to point
		// the bulk importer at that directory and not import any unwanted crap
		String fullObjectNumber = PathTools.addNumericPaths(paths, object.getNumber());

		String appendix = String.format("%08x", info.getIndex());
		String baseName = fullObjectNumber;

		return newLocation(paths, baseName, null, null, appendix);
	}
}