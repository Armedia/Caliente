/**
 *
 */

package com.delta.cmsmf.cms;

/**
 * @author diego
 *
 */
public enum CmsDependencyType {
	/**
	 * <p>
	 * There are no dependencies with objects of the same type.
	 * </p>
	 */
	NONE,

	/**
	 * <p>
	 * There are peer-level relationships with objects of the same type, so while import/export
	 * order is unimportant (because there may be loops), it's necessary to execute a 2nd pass over
	 * the objects to resolve those relationships.
	 * </p>
	 */
	PEER,

	/**
	 * <p>
	 * There are hierarchical dependencies with objects of the same type, so export/import order is
	 * important. The export/import may not be parallelized.
	 * </p>
	 */
	HIERARCHY;
}