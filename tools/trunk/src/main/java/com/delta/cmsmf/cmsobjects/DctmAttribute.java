package com.delta.cmsmf.cmsobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataType;
import com.documentum.fc.common.IDfValue;

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

	/** The repeating values. */
	private List<Object> repeatingValues;

	/** The single value. */
	private Object singleValue;

	/** The attribute value type. */
	private DctmAttributeTypesEnum attrValueType;

	/**
	 * Instantiates a new dctm attribute.
	 */
	public DctmAttribute() {
	}

	DctmAttribute(DataAttribute attribute) {
		this.attrValueType = attribute.isRepeating() ? DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE
			: DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE;
		DataType type = attribute.getType();
		if (attribute.isRepeating()) {
			this.repeatingValues = new ArrayList<Object>(attribute.getValueCount());
			for (IDfValue v : attribute) {
				this.repeatingValues.add(type.getValue(v));
			}
		} else {
			this.singleValue = type.getValue(attribute.getValue(0));
			this.repeatingValues = null;
		}
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
