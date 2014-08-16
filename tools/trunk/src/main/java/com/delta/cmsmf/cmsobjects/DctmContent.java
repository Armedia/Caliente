package com.delta.cmsmf.cmsobjects;

import java.io.IOException;

import org.apache.log4j.Level;

import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

// TODO: Auto-generated Javadoc
/**
 * The DctmContent class represents a content file of a dm_document object from CMS.
 * This class contains fields to store the file format and page number of a content file.
 * The contentByteArray field stores the entire content file in byte[] format.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class DctmContent extends DctmObject {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new DctmContent object.
	 */
	public DctmContent() {
		super();
		// set dctmObjectType to dctm_format
		this.dctmObjectType = DctmObjectTypesEnum.DCTM_CONTENT;
	}

	/**
	 * Instantiates a DctmContent object with new CMS session.
	 * 
	 * @param dctmSession
	 *            the existing documentum CMS session
	 */
	public DctmContent(IDfSession dctmSession) {
		super(dctmSession);
	}

	/** The page nbr of a content file. */
	private int pageNbr;

	/**
	 * Gets the page nbr of a content file.
	 * 
	 * @return the page nbr
	 */
	public int getPageNbr() {
		return this.pageNbr;
	}

	/**
	 * Sets the page nbr of a content file.
	 * 
	 * @param pageNbr
	 *            the new page nbr
	 */
	public void setPageNbr(int pageNbr) {
		this.pageNbr = pageNbr;
	}

	/** The page modifier of a content file. */
	private String pageModifier;

	/**
	 * Gets the page modifier of a content file.
	 * 
	 * @return the page modifier
	 */
	public String getPageModifier() {
		return this.pageModifier;
	}

	/**
	 * Sets the page modifier of a content file.
	 * 
	 * @param pageModifier
	 *            the new page modifier
	 */
	public void setPageModifier(String pageModifier) {
		this.pageModifier = pageModifier;
	}

	/** The content byte array that stores the content file in binary format. */
	private byte[] contentByteArray;

	/**
	 * Gets the content byte array of a content file.
	 * 
	 * @return the content byte array
	 */
	public byte[] getContentByteArray() {
		return this.contentByteArray;
	}

	/**
	 * Sets the content byte array of a content file.
	 * 
	 * @param contentByteArray
	 *            the new content byte array
	 */
	public void setContentByteArray(byte[] contentByteArray) {
		this.contentByteArray = contentByteArray;
	}

	/** The relative content file location. */
	private String relativeContentFileLocation;

	/**
	 * Gets the relative content file location.
	 * 
	 * @return the relative content file location
	 */
	public String getRelativeContentFileLocation() {
		return this.relativeContentFileLocation;
	}

	/**
	 * Sets the relative content file location.
	 * 
	 * @param relativeContentFileLocation
	 *            the new relative content file location
	 */
	public void setRelativeContentFileLocation(String relativeContentFileLocation) {
		this.relativeContentFileLocation = relativeContentFileLocation;
	}

	@Override
	public void createInCMS() throws DfException, IOException {
		// This method is left empty intentionally. We will create the content
		// objects within the dctmdocument createInCMS() method.
	}

	@Override
	public DctmObject getFromCMS(IDfPersistentObject prsstntObj) throws CMSMFException {
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Started getting dctm dmr_content object from repository");
		}
		String contentID = "";
		try {
			contentID = prsstntObj.getObjectId().getId();

			DctmContent dctmContent = new DctmContent();
			getAllAttributesFromCMS(dctmContent, prsstntObj, contentID);
			if (DctmObject.logger.isEnabledFor(Level.INFO)) {
				DctmObject.logger
					.info("Finished getting dctm dmr_content object from repository with id: " + contentID);
			}

			return dctmContent;
		} catch (DfException e) {
			throw (new CMSMFException("Error retrieving format in repository with id: " + contentID, e));
		}
	}
}
