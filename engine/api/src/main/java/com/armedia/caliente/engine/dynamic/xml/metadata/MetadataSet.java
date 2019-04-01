package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ShareableLockable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataSet.t", propOrder = {
	"loaders"
})
public class MetadataSet implements ShareableLockable {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElements({
		@XmlElement(name = "from-sql", type = MetadataFromSQL.class),
		@XmlElement(name = "from-ddl", type = MetadataFromDDL.class)
	})
	protected List<AttributeValuesLoader> loaders;

	@XmlAttribute(name = "id", required = true)
	protected String id;

	@XmlAttribute(name = "failOnError", required = false)
	protected Boolean failOnError;

	@XmlAttribute(name = "failOnMissing", required = false)
	protected Boolean failOnMissing;

	@XmlTransient
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	private List<AttributeValuesLoader> initializedLoaders;

	@XmlTransient
	private Map<String, MetadataSource> dataSources;

	@Override
	public ReadWriteLock getShareableLock() {
		return this.rwLock;
	}

	public List<AttributeValuesLoader> getLoaders() {
		if (this.loaders == null) {
			this.loaders = new ArrayList<>();
		}
		return this.loaders;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isFailOnError() {
		return Tools.coalesce(this.failOnError, Boolean.FALSE);
	}

	public void setFailOnError(Boolean value) {
		this.failOnError = value;
	}

	public boolean isFailOnMissing() {
		return Tools.coalesce(this.failOnMissing, Boolean.FALSE);
	}

	public void setFailOnMissing(Boolean value) {
		this.failOnMissing = value;
	}

	public void initialize(Map<String, MetadataSource> ds) throws Exception {
		shareLockedUpgradable(() -> this.initializedLoaders, Objects::isNull, (e) -> {
			if (this.initializedLoaders != null) { return; }
			List<AttributeValuesLoader> initializedLoaders = new ArrayList<>();
			Map<String, MetadataSource> dataSources = new HashMap<>();
			boolean ok = false;
			try {
				for (AttributeValuesLoader loader : getLoaders()) {
					if (loader == null) {
						continue;
					}

					final String dsName = loader.getDataSource();
					final MetadataSource mds = ds.get(dsName);
					if (mds == null) {
						throw new Exception(String.format(
							"A %s loader in MetadataSet %s references a non-existent metadata source [%s]",
							loader.getClass().getSimpleName(), getId(), dsName));
					}
					try (final Connection c = mds.getConnection()) {
						loader.initialize(c);
						initializedLoaders.add(loader);
						dataSources.put(dsName, mds);
					}
				}
				ok = true;
			} finally {
				if (!ok) {
					// Close out all the initialized loaders, just before rolling back
					for (AttributeValuesLoader loader : initializedLoaders) {
						try {
							loader.close();
						} catch (Throwable t) {
							this.log.warn("Failed to close an initialized {} loader in MetadataSet {}",
								loader.getClass().getSimpleName(), getId(), t);
						}
					}
				}
			}
			this.initializedLoaders = Tools.freezeList(initializedLoaders);
			this.dataSources = Tools.freezeMap(dataSources);
		});
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object) throws Exception {
		return shareLocked(() -> {
			// If there are no loades initialized, this is a problem...
			if (this.initializedLoaders == null) { throw new Exception("This metadata source is not yet initialized"); }

			// If there are no loaders initialized, we always return empty
			if (this.initializedLoaders.isEmpty()) { return null; }

			Map<String, CmfAttribute<V>> finalAttributes = new HashMap<>();
			for (AttributeValuesLoader l : this.initializedLoaders) {
				if (l == null) {
					continue;
				}

				MetadataSource mds = this.dataSources.get(l.getDataSource());
				Map<String, CmfAttribute<V>> newAttributes = null;
				try (Connection c = mds.getConnection()) {
					newAttributes = l.getAttributeValues(c, object);
				} catch (Exception e) {
					if (isFailOnError()) {
						// An exceptikon was caught, but we need to fail on it
						throw new Exception(
							String.format("Exception raised while loading external metadata attributes for %s",
								object.getDescription()),
							e);
					} else {
						// TODO: Log this exception anyway...
					}
				}

				if ((newAttributes == null) && isFailOnMissing()) {
					// The attribute values are required, but none were found...this is an
					// error!
					throw new Exception(String.format("Did not find the required external metadata attributes for %s",
						object.getDescription()));
				}

				if (newAttributes != null) {
					finalAttributes.putAll(newAttributes);
				}
			}
			if (finalAttributes.isEmpty()) {
				finalAttributes = null;
			}
			return finalAttributes;
		});
	}

	public void close() {
		shareLockedUpgradable(() -> this.initializedLoaders, Objects::nonNull, (e) -> {
			if (this.initializedLoaders == null) { return; }
			for (AttributeValuesLoader loader : this.initializedLoaders) {
				try {
					loader.close();
				} catch (Throwable t) {
					this.log.error("Exception caught closing a {} attribute loader in MetadataSet {}",
						loader.getClass().getSimpleName(), getId(), t);
				}
			}
			this.initializedLoaders = null;
		});
	}
}