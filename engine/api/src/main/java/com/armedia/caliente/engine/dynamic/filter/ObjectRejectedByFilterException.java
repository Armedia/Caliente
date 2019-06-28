/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.dynamic.filter;

import com.armedia.caliente.engine.dynamic.RuntimeDynamicElementException;

public class ObjectRejectedByFilterException extends RuntimeDynamicElementException {
	private static final long serialVersionUID = 1L;

	public ObjectRejectedByFilterException() {
	}

	public ObjectRejectedByFilterException(String message) {
		super(message);
	}

	public ObjectRejectedByFilterException(Throwable cause) {
		super(cause);
	}

	public ObjectRejectedByFilterException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectRejectedByFilterException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}