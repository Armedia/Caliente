package com.armedia.caliente.engine.xml;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.ActionException;
import com.armedia.caliente.engine.transform.ObjectContext;

@XmlTransient
public interface Action {

	public void apply(ObjectContext ctx) throws ActionException;

}