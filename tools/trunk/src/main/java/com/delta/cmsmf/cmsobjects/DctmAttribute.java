package com.delta.cmsmf.cmsobjects;

import java.io.Serializable;
import java.util.List;

/**
 * The DctmUser DctmAttribute represents an attribute or a property of any object in CMS.
 * This class contains fields to contain value of single value type or repeating value
 * type of attributes. Field attrValueType determines whether the type of attribute is
 * single value or repeating value. Repeating values are stored in a List structure.
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmAttribute implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new dctm attribute.
	 */
	public DctmAttribute() {
		super();
	}

	/**
	 * Instantiates a new dctm attribute.
	 *
	 * @param attrValType
	 *            the attribute value type
	 */
	public DctmAttribute(DctmAttributeTypesEnum attrValType) {
		this.attrValueType = attrValType;
	}

	/** The repeating values. */
	private List<Object> repeatingValues;

	/**
	 * Gets the repeating values of this attribute.
	 *
	 * @return the repeating values
	 */
	public List<Object> getRepeatingValues() {
		return this.repeatingValues;
	}

	/**
	 * Sets the repeating values of this attribute.
	 *
	 * @param repeatingValues
	 *            the new repeating values
	 */
	public void setRepeatingValues(List<Object> repeatingValues) {
		this.repeatingValues = repeatingValues;
	}

	/** The single value. */
	private Object singleValue;

	/**
	 * Gets the single value of this attribute.
	 *
	 * @return the single value
	 */
	public Object getSingleValue() {
		return this.singleValue;
	}

	/**
	 * Sets the single value of this attribute.
	 *
	 * @param singleValue
	 *            the new single value
	 */
	public void setSingleValue(Object singleValue) {
		this.singleValue = singleValue;
	}

	/** The attribute value type. */
	private DctmAttributeTypesEnum attrValueType;

	/**
	 * Gets the attr value type.
	 *
	 * @return the attribute value type
	 */
	public DctmAttributeTypesEnum getAttrValueType() {
		return this.attrValueType;
	}

	/**
	 * Sets the attribute value type.
	 *
	 * @param attrType
	 *            the new attribute value type
	 */
	public void setAttrValueType(DctmAttributeTypesEnum attrType) {
		this.attrValueType = attrType;
	}

	/**
	 * Removes the repeating attribute value from the value list of this attribute.
	 *
	 * @param attrValue
	 *            the attribute value that needs to be removed
	 * @return true, if successful
	 */
	public boolean removeRepeatingAttrValue(Object attrValue) {
		if (this.attrValueType == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) {
			return this.repeatingValues.remove(attrValue);
		} else {
			return false;
		}
	}

	/**
	 * Gets the repeating values of this attribute in comma separated string format.
	 *
	 * @return the repeating values as comma separated string
	 */
	public String getRepeatingValuesAsCommaSeparatedString() {
		StringBuffer returnString = new StringBuffer("");

		for (int i = 0; i < this.repeatingValues.size(); i++) {
			if (i == 0) {
				returnString.append(this.repeatingValues.get(i).toString());
			} else {
				returnString.append("," + this.repeatingValues.get(i).toString());
			}
		}
		return returnString.toString();
	}
}
