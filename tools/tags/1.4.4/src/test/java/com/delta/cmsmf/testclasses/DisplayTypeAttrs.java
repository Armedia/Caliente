package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

// TODO: Auto-generated Javadoc
/**
 * The Class DisplayTypeAttrs.
 */
public class DisplayTypeAttrs {

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	public static void main(String[] args) throws DfException {
		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		IDfType typeObj = dctmSession.getType("dm_folder");
		// int superTypeAttrcount = typeObj.getSuperType().getTypeAttrCount();
		System.out.println("super document type length: " + typeObj.getSuperType().getTypeAttrCount());
		System.out.println("custom document type length: " + typeObj.getTypeAttrCount());
		int startPosition = typeObj.getInt("start_pos");
		System.out.println("startPosition : " + startPosition);

		for (int i = startPosition; i < typeObj.getTypeAttrCount(); i++) {
			// for (int i = superTypeAttrcount; i < typeObj.getTypeAttrCount(); i++) {
			IDfAttr attr = typeObj.getTypeAttr(i);
			System.out.println("Attribute Name: " + attr.getName() + " isRepeating: " + attr.isRepeating()
				+ " Length: " + attr.getLength());

		}
	}

}
