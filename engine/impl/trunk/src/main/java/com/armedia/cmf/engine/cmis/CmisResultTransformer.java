package com.armedia.cmf.engine.cmis;

import org.apache.chemistry.opencmis.client.api.QueryResult;

public interface CmisResultTransformer<E> {

	public E transform(QueryResult result) throws Exception;

}