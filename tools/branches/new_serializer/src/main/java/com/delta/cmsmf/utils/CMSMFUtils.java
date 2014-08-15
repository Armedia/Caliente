package com.delta.cmsmf.utils;

import java.io.File;

public class CMSMFUtils {

	public static String GetContentPathFromContentID(String contentObjID) {
		String contentPath = "";
		String filePathSeparator = File.separator;
		if (contentObjID.length() == 16) {
			// 16 character object id in dctm consists of first 2 chars of obj type, next 6 chars of
// docbase
			// id in hex and last 8 chars server generated. We will use first 6 characters of this
// last 8
			// characters and generate the unique path.
			// For ex: if the id is 0600a92b80054db8 than the path would be 80\05\4d
			contentPath = contentObjID.substring(8, 16);
			contentPath = new StringBuffer(contentPath.substring(0, 2)).append(filePathSeparator)
				.append(contentPath.subSequence(2, 4)).append(filePathSeparator).append(contentPath.subSequence(4, 6))
				.toString();
		}

		return contentPath;

	}

}
