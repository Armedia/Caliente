package com.delta.cmsmf.cmsobjects;

import java.io.Serializable;

// TODO: Auto-generated Javadoc
/**
 * The DctmContent class represents a content file of a dm_document object from CMS.
 * This class contains fields to store the file format and page number of a content file.
 * The contentByteArray field stores the entire content file in byte[] format.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class DctmContent implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The content format of a content file. */
	private String contentFormat;

	/**
	 * Gets the content format of a content file.
	 * 
	 * @return the content format
	 */
	public String getContentFormat() {
		return this.contentFormat;
	}

	/**
	 * Sets the content format of a content file.
	 * 
	 * @param contentFormat
	 *            the new content format
	 */
	public void setContentFormat(String contentFormat) {
		this.contentFormat = contentFormat;
	}

	/** The rendition nbr. */
	private int renditionNbr;

	/**
	 * Gets the rendition nbr.
	 * 
	 * @return the rendition nbr
	 */
	public int getRenditionNbr() {
		return this.renditionNbr;
	}

	/**
	 * Sets the rendition nbr.
	 * 
	 * @param renditionNbr
	 *            the new rendition nbr
	 */
	public void setRenditionNbr(int renditionNbr) {
		this.renditionNbr = renditionNbr;
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

	/** The set_file attribute of a content file. */
	private String setFile;

	/**
	 * Gets the set_file attribute of a content file.
	 * 
	 * @return the set_file attribute
	 */
	public String getSetFile() {
		return this.setFile;
	}

	/**
	 * Sets the set_file attribute of a content file.
	 * 
	 * @param setFile
	 *            the new set_file attribute
	 */
	public void setSetFile(String setFile) {
		this.setFile = setFile;
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
}
