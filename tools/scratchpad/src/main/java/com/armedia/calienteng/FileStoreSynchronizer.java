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
package com.armedia.calienteng;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.armedia.commons.utilities.CfgTools;

public class FileStoreSynchronizer {

	public static enum Model {
		//
		MASTER_A, //
		MASTER_B, //
		MIRROR, //
		//
		;
	}

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final FileStore alpha;
	private final FileStore bravo;

	public FileStoreSynchronizer(CfgTools configuration, FileStore alpha, FileStore bravo, Model model)
		throws IOException {
		if ((alpha == null) || (bravo == null)) {
			throw new IllegalArgumentException("Must provide both FileStore instances");
		}
		if (model == null) { throw new IllegalArgumentException("Must provide a valid mode of operation"); }
		this.alpha = alpha;
		this.bravo = bravo;

		/*
			// On "watched" objects
			dm_checkin
			dm_destroy
			dm_branch
			dm_prune
			dm_save
			dm_unlink
		
			// On all dm_folder and dm_document
			dm_link
		 */
	}

	protected final Lock getReadLock() {
		return this.lock.readLock();
	}

	protected final Lock getWriteLock() {
		return this.lock.writeLock();
	}

	public void start() {
	}

	public FileStore getFileStoreAlpha() {
		return this.alpha;
	}

	public FileStore getFileStoreBravo() {
		return this.bravo;
	}

	public Model getModel() {
		return null;
	}

	public void stop() {
	}
}