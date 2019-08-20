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
package com.armedia.caliente.engine.dynamic;

import java.util.HashSet;
import java.util.Set;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.store.CmfObject;

public class TestObjectFacade extends DynamicObject {

	private String objectId = null;
	private final String historyId = null;
	private boolean historyCurrent = false;
	private CmfObject.Archetype type = null;
	private String label = null;
	private String originalSubtype = null;
	private Set<String> originalSecondaries = new HashSet<>();
	private Set<String> secondaries = new HashSet<>();
	private int dependencyTier = 0;
	private String originalName = null;

	public TestObjectFacade() {
	}

	@Override
	public String getObjectId() {
		return this.objectId;
	}

	public TestObjectFacade setObjectId(String objectId) {
		this.objectId = objectId;
		return this;
	}

	@Override
	public boolean isHistoryCurrent() {
		return this.historyCurrent;
	}

	public TestObjectFacade setHistoryCurrent(boolean historyCurrent) {
		this.historyCurrent = historyCurrent;
		return this;
	}

	@Override
	public CmfObject.Archetype getType() {
		return this.type;
	}

	@Override
	public String getLabel() {
		return this.label;
	}

	public TestObjectFacade setLabel(String label) {
		this.label = label;
		return this;
	}

	public TestObjectFacade setType(CmfObject.Archetype type) {
		this.type = type;
		return this;
	}

	@Override
	public String getOriginalSubtype() {
		return this.originalSubtype;
	}

	public TestObjectFacade setOriginalSubtype(String originalSubtype) {
		this.originalSubtype = originalSubtype;
		return this;
	}

	@Override
	public TestObjectFacade setSubtype(String subtype) {
		super.setSubtype(subtype);
		return this;
	}

	@Override
	public Set<String> getOriginalSecondarySubtypes() {
		return this.originalSecondaries;
	}

	public TestObjectFacade setOriginalSecondarySubtypes(Set<String> originalSecondaries) {
		this.originalSecondaries = originalSecondaries;
		return this;
	}

	@Override
	public Set<String> getSecondarySubtypes() {
		return this.secondaries;
	}

	public TestObjectFacade setSecondarySubtypes(Set<String> secondaries) {
		this.secondaries = secondaries;
		return this;
	}

	@Override
	public int getDependencyTier() {
		return this.dependencyTier;
	}

	public TestObjectFacade setDependencyTier(int dependencyTier) {
		this.dependencyTier = dependencyTier;
		return this;
	}

	@Override
	public TestObjectFacade setName(String name) {
		super.setName(name);
		return this;
	}

	@Override
	public String getHistoryId() {
		return this.historyId;
	}

	public TestObjectFacade setOriginalName(String originalName) {
		this.originalName = originalName;
		return this;
	}

	@Override
	public String getOriginalName() {
		return this.originalName;
	}
}