package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.caliente.engine.dynamic.ScriptableObjectFacade;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObject.Archetype;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.commons.utilities.Tools;

class MetadataObjectFacade<V> implements ScriptableObjectFacade<MetadataPropertyFacade> {

	private final CmfObject<V> object;
	private final Map<String, MetadataPropertyFacade> att;
	private final Map<String, MetadataPropertyFacade> priv;

	MetadataObjectFacade(CmfObject<V> object) {
		this.object = Objects.requireNonNull(object);
		final CmfAttributeTranslator<V> translator = object.getTranslator();
		Map<String, MetadataPropertyFacade> m = new TreeMap<>();
		for (CmfAttribute<V> att : this.object.getAttributes()) {
			m.put(att.getName(), new MetadataPropertyFacade(att, translator));
		}
		this.att = Tools.freezeMap(new LinkedHashMap<>(m));
		m = new TreeMap<>();
		for (CmfProperty<V> att : this.object.getProperties()) {
			m.put(att.getName(), new MetadataPropertyFacade(att, translator));
		}
		this.priv = Tools.freezeMap(new LinkedHashMap<>(m));
	}

	@Override
	public Map<String, MetadataPropertyFacade> getAtt() {
		return this.att;
	}

	@Override
	public Map<String, MetadataPropertyFacade> getPriv() {
		return this.priv;
	}

	@Override
	public Archetype getType() {
		return this.object.getType();
	}

	@Override
	public String getObjectId() {
		return this.object.getId();
	}

	@Override
	public String getName() {
		return this.object.getName();
	}

	@Override
	public String getOriginalName() {
		return this.object.getName();
	}

	@Override
	public String getSubtype() {
		return this.object.getSubtype();
	}

	@Override
	public String getOriginalSubtype() {
		return this.object.getSubtype();
	}

	@Override
	public int getDependencyTier() {
		return this.object.getDependencyTier();
	}

	@Override
	public String getHistoryId() {
		return this.object.getHistoryId();
	}

	@Override
	public boolean isHistoryCurrent() {
		return this.object.isHistoryCurrent();
	}

	@Override
	public String getLabel() {
		return this.object.getLabel();
	}

	@Override
	public Set<String> getSecondarySubtypes() {
		return this.object.getSecondarySubtypes();
	}

	@Override
	public Set<String> getOriginalSecondarySubtypes() {
		return this.object.getSecondarySubtypes();
	}
}