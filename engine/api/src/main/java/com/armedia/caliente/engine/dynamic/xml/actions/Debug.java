/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/

package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Action;
import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ActionGroup;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionDebug.t", propOrder = {
	"actions"
})
public class Debug extends ConditionalAction {

	@XmlElements({
		// The polymorphic children...
		@XmlElement(name = "group", type = ActionGroup.class), //
		@XmlElement(name = "abort-transformation", type = AbortTransformation.class), //
		@XmlElement(name = "add-secondary-subtype", type = SecondarySubtypeAdd.class), //
		@XmlElement(name = "apply-value-mapping", type = ValueMappingApply.class), //
		@XmlElement(name = "clear-value-mapping", type = ValueMappingClear.class), //
		@XmlElement(name = "copy-attribute", type = AttributeActions.Copy.class), //
		@XmlElement(name = "copy-internal-property", type = InternalPropertyActions.Copy.class), //
		@XmlElement(name = "copy-variable", type = VariableActions.Copy.class), //
		@XmlElement(name = "custom-action", type = CustomAction.class), //
		@XmlElement(name = "end-transformation", type = EndTransformation.class), //
		@XmlElement(name = "join-attribute", type = AttributeActions.Join.class), //
		@XmlElement(name = "join-internal-property", type = InternalPropertyActions.Join.class), //
		@XmlElement(name = "join-variable", type = VariableActions.Join.class), //
		@XmlElement(name = "load-external-metadata", type = LoadExternalMetadata.class), //
		@XmlElement(name = "map-attribute-value", type = AttributeActions.MapValue.class), //
		@XmlElement(name = "map-internal-property-value", type = InternalPropertyActions.MapValue.class), //
		@XmlElement(name = "map-original-subtype", type = MapOriginalSubtype.class), //
		@XmlElement(name = "map-principal", type = PrincipalMappingApply.class), //
		@XmlElement(name = "map-subtype", type = MapSubtype.class), //
		@XmlElement(name = "map-variable-value", type = VariableActions.MapValue.class), //
		@XmlElement(name = "remove-attribute", type = AttributeActions.Remove.class), //
		@XmlElement(name = "remove-internal-property", type = InternalPropertyActions.Remove.class), //
		@XmlElement(name = "remove-original-secondary-subtypes", type = OriginalSecondarySubtypeRemove.class), //
		@XmlElement(name = "remove-secondary-subtype", type = SecondarySubtypeRemove.class), //
		@XmlElement(name = "remove-variable", type = VariableActions.Remove.class), //
		@XmlElement(name = "rename-attribute", type = AttributeActions.Rename.class), //
		@XmlElement(name = "rename-internal-property", type = InternalPropertyActions.Rename.class), //
		@XmlElement(name = "rename-variable", type = VariableActions.Rename.class), //
		@XmlElement(name = "replace-attribute", type = AttributeActions.Replace.class), //
		@XmlElement(name = "replace-internal-property", type = InternalPropertyActions.Replace.class), //
		@XmlElement(name = "replace-secondary-subtype", type = SecondarySubtypeReplace.class), //
		@XmlElement(name = "replace-subtype", type = SubtypeReplace.class), //
		@XmlElement(name = "replace-variable", type = VariableActions.Replace.class), //
		@XmlElement(name = "reset-original-secondary-subtypes", type = OriginalSecondarySubtypeReset.class), //
		@XmlElement(name = "set-attribute", type = AttributeActions.Set.class), //
		@XmlElement(name = "set-internal-property", type = InternalPropertyActions.Set.class), //
		@XmlElement(name = "set-subtype", type = SubtypeSet.class), //
		@XmlElement(name = "set-value-mapping", type = ValueMappingSet.class), //
		@XmlElement(name = "set-variable", type = VariableActions.Set.class), //
		@XmlElement(name = "split-attribute", type = AttributeActions.Split.class), //
		@XmlElement(name = "split-internal-property", type = InternalPropertyActions.Split.class), //
		@XmlElement(name = "split-variable", type = VariableActions.Split.class), //
	})
	protected List<Action> actions;

	public List<Action> getActions() {
		if (this.actions == null) {
			this.actions = new ArrayList<>();
		}
		return this.actions;
	}

	@Override
	protected boolean isSkippable() {
		// This action can't be skipped
		return false;
	}

	@Override
	protected final void executeAction(DynamicElementContext<?> ctx) throws ActionException {
		// Allow for debugging of the child action elements...
		Collection<Action> actions = getActions();
		for (Action action : actions) {
			if (action != null) {
				action.apply(ctx);
			}
		}
	}

}