package com.delta.cmsmf.mainEngine;

import java.io.IOException;

import com.delta.cmsmf.exception.CMSMFException;

public interface CMSMFMain {

	public void run() throws IOException, CMSMFException;

	public boolean requiresDataStore();

	public boolean requiresCleanData();
}