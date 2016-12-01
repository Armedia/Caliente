package com.armedia.caliente.store;

public interface CmfTreeScanner {

	public void scanNode(CmfObjectRef parent, CmfObjectRef child, String name) throws Exception;

}