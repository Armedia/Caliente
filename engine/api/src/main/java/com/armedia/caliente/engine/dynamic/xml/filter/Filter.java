package com.armedia.caliente.engine.dynamic.xml.filter;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.Action;
import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filter.t", propOrder = {
	"actions"
})
public class Filter extends ConditionalAction {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElements({
		@XmlElement(name = "reject-object", type = ObjectReject.class), //
		@XmlElement(name = "accept-object", type = ObjectAccept.class), //
	})
	protected List<Action> actions;

	public List<Action> getActions() {
		if (this.actions == null) {
			this.actions = new ArrayList<>();
		}
		return this.actions;
	}

	@Override
	protected final void executeAction(DynamicElementContext ctx) throws ActionException {
		for (Action action : getActions()) {
			if (action != null) {
				action.apply(ctx);
			}
		}
	}

}