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
		if ((alpha == null)
			|| (bravo == null)) { throw new IllegalArgumentException("Must provide both FileStore instances"); }
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