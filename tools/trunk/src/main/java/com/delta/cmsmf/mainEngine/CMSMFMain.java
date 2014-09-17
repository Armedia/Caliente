package com.delta.cmsmf.mainEngine;

import com.delta.cmsmf.exception.CMSMFException;

public interface CMSMFMain {

	public void run() throws CMSMFException;

	public boolean requiresDataStore();

	public boolean requiresCleanData();
}