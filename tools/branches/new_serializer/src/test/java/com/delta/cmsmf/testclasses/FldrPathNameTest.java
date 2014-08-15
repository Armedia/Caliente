package com.delta.cmsmf.testclasses;

import com.documentum.fc.common.DfException;

// TODO: Auto-generated Javadoc
/**
 * The Class DupObjectTest.
 */
public class FldrPathNameTest {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {

		String fldrPath = "/1 - Continuity of Business/Recovery Teams/COB/COB DR CD";

		fldrPath = fldrPath.substring(0, fldrPath.lastIndexOf("/"));
		System.out.println(fldrPath);
		System.out.println("Done!!");

	}

}
