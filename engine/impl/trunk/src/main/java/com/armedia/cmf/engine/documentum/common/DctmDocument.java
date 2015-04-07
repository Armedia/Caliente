/**
 *
 */

package com.armedia.cmf.engine.documentum.common;

import com.documentum.fc.common.IDfTime;

/**
 * @author diego
 *
 */
public interface DctmDocument extends DctmSysObject {

	static final String CONTENTS = "contents";

	static final String CONTENT_INFO = "contentInfo";

	static final String CONTENT_SET_TIME_PATTERN = IDfTime.DF_TIME_PATTERN46;
}