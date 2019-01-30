package com.armedia.caliente.store;

@FunctionalInterface
public interface CmfTreeScanner {

	public void scanNode(CmfObjectRef parent, CmfObjectRef child, String name) throws Exception;

}