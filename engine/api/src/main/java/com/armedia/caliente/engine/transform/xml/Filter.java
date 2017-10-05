package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.actions.ObjectFail;
import com.armedia.caliente.engine.transform.xml.actions.ObjectProcess;
import com.armedia.caliente.engine.transform.xml.actions.ObjectSkip;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filter.t", propOrder = {
	"filters"
})
public class Filter extends ConditionalAction {

	@XmlElements({
		@XmlElement(name = "process-object", type = ObjectProcess.class), //
		@XmlElement(name = "skip-object", type = ObjectSkip.class), //
		@XmlElement(name = "fail-object", type = ObjectFail.class), //
	})
	protected List<Action> filters;

	public List<Action> getFilters() {
		if (this.filters == null) {
			this.filters = new ArrayList<>();
		}
		return this.filters;
	}

	@Override
	protected boolean isSkippable() {
		// Allow skipping if there are no actions contained in this group
		return super.isSkippable() || (this.filters == null) || this.filters.isEmpty();
	}

	@Override
	protected final void applyTransformation(TransformationContext ctx) throws TransformationException {
		for (Action filter : getFilters()) {
			if (filter != null) {
				filter.apply(ctx);
			}
		}
	}

}