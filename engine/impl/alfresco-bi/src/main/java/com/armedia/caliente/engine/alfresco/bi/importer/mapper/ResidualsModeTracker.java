package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.ResidualsMode;
import com.armedia.commons.utilities.Tools;

class ResidualsModeTracker {
	static final ResidualsMode DEFAULT_MODE = ResidualsMode.EXCLUDE;
	private ResidualsMode mode = null;

	ResidualsModeTracker() {
		this(null);
	}

	ResidualsModeTracker(ResidualsMode initial) {
		this.mode = Tools.coalesce(initial, ResidualsModeTracker.DEFAULT_MODE);
	}

	public void applyResidualsMode(ResidualsMode mode) {
		if (mode == null) { return; }
		if (mode == this.mode) { return; }
		switch (this.mode) {
			case MANDATORY:
			case REJECT:
				// Already at a final state, so no change is allowed...
				switch (mode) {
					case MANDATORY:
					case REJECT:
						// TODO: Should we issue a warning? Raise an error?
					default:
						break;
				}
				return;

			default:
				break;
		}
		this.mode = mode;
	}

	public ResidualsMode getActiveResidualsMode() {
		return this.mode;
	}

}