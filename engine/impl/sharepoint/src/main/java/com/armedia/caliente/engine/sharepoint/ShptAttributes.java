/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.sharepoint;

public enum ShptAttributes {

	//
	OBJECT_ID,
	OBJECT_CLASS,
	OBJECT_TYPE,
	OBJECT_ISSUER,
	OBJECT_NAME,
	SITE_ADMIN,
	PRINCIPAL_TYPE,
	PRINCIPAL_ID,
	PRINCIPAL_ID_ISSUER,
	EMAIL,
	TITLE,
	USER_GROUPS,
	USER_ROLES,
	AUTO_ACCEPT_MEMBERSHIP_REQUEST,
	ALLOW_MEMBERSHIP_REQUEST,
	ALLOW_MEMBERS_EDIT_MEMBERSHIP,
	GROUP_MEMBERS,
	OWNER_TITLE,
	GROUP_OWNER,
	DESCRIPTION,
	CONTENT_TYPE,
	CONTENT_SIZE,
	CONTENT_HASH,
	OWNER,
	OWNER_PERMISSION,
	GROUP,
	GROUP_PERMISSION,
	CREATOR,
	CREATE_DATE,
	MODIFIER,
	MODIFICATION_DATE,
	ACCESSOR,
	ACCESS_DATE,
	PARENTS,
	PATHS,
	LOGIN_NAME,
	LOGIN_DOMAIN,
	HOME_FOLDER,
	WELCOME_PAGE,
	VERSION,
	VERSION_TREE,
	VERSION_PRIOR,
	//
	;

	public final String name;

	private ShptAttributes() {
		this.name = name().toLowerCase();
	}
}