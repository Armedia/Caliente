package com.delta.cmsmf.mainEngine;

import java.io.IOException;

import com.delta.cmsmf.exception.CMSMFFatalException;

public interface CMSMFMain {

	public void run() throws IOException, CMSMFFatalException;

	public boolean requiresDataStore();

	public boolean requiresCleanData();
}