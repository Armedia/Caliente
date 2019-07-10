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
package com.armedia.caliente.store.tools;

import java.util.BitSet;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfStorageException;

public class DefaultCmfObjectHandler<VALUE> implements CmfObjectHandler<VALUE> {

	public static enum Flag {
		//
		NEW_TIER(true), //
		NEW_HISTORY(true), //
		HANDLE_OBJECT(true), //
		HANDLE_EXCEPTION(false), //
		END_HISTORY(true), //
		END_TIER(true), //
		//
		;

		private final boolean defaultValue;

		private Flag(boolean defaultValue) {
			this.defaultValue = defaultValue;
		}

		public boolean set(BitSet bits) {
			if (bits == null) { throw new IllegalArgumentException("Must provide a bit set to manipulate"); }
			boolean current = get(bits);
			bits.set(ordinal());
			return current;
		}

		public boolean clear(BitSet bits) {
			if (bits == null) { throw new IllegalArgumentException("Must provide a bit set to manipulate"); }
			boolean current = get(bits);
			bits.clear(ordinal());
			return current;
		}

		public boolean toggle(BitSet bits) {
			if (bits == null) { throw new IllegalArgumentException("Must provide a bit set to manipulate"); }
			boolean current = get(bits);
			bits.flip(ordinal());
			return current;
		}

		public boolean get(BitSet bits) {
			if (bits == null) { return this.defaultValue; }
			return bits.get(ordinal());
		}
	}

	protected final boolean retNewTier;
	protected final boolean retNewHistory;
	protected final boolean retHandleObject;
	protected final boolean retHandleException;
	protected final boolean retEndHistory;
	protected final boolean retEndTier;

	public DefaultCmfObjectHandler() {
		this(null);
	}

	public DefaultCmfObjectHandler(BitSet flags) {
		this.retNewTier = Flag.NEW_TIER.get(flags);
		this.retNewHistory = Flag.NEW_HISTORY.get(flags);
		this.retHandleObject = Flag.HANDLE_OBJECT.get(flags);
		this.retHandleException = Flag.HANDLE_EXCEPTION.get(flags);
		this.retEndHistory = Flag.END_HISTORY.get(flags);
		this.retEndTier = Flag.END_TIER.get(flags);
	}

	@Override
	public boolean newTier(int tierNumber) throws CmfStorageException {
		return this.retNewTier;
	}

	@Override
	public boolean newHistory(String historyId) throws CmfStorageException {
		return this.retNewHistory;
	}

	@Override
	public boolean handleObject(CmfObject<VALUE> dataObject) throws CmfStorageException {
		return this.retHandleObject;
	}

	@Override
	public boolean handleException(Exception e) {
		return this.retHandleException;
	}

	@Override
	public boolean endHistory(String historyId, boolean ok) throws CmfStorageException {
		return this.retEndHistory;
	}

	@Override
	public boolean endTier(int tierNumber, boolean ok) throws CmfStorageException {
		return this.retEndTier;
	}
}