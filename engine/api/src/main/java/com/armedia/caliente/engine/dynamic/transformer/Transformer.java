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
package com.armedia.caliente.engine.dynamic.transformer;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DefaultDynamicObject;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.ProcessingCompletedException;
import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.engine.dynamic.transformer.mapper.AttributeMapper;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.dynamic.xml.Transformations;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

public class Transformer extends BaseShareableLockable {

	private static final XmlInstances<Transformations> INSTANCES = new XmlInstances<>(Transformations.class);

	public static Transformer getTransformer(String location, ExternalMetadataLoader metadataLoader,
		AttributeMapper attributeMapper, boolean failIfMissing) throws TransformerException {
		try {
			try {
				Transformations transformations = Transformer.INSTANCES.getInstance(location);
				if (transformations == null) { return null; }
				return new Transformer(location, transformations, metadataLoader, attributeMapper);
			} catch (final XmlNotFoundException e) {
				if (!failIfMissing) { return null; }
				throw e;
			}
		} catch (Exception e) {
			String pre = "";
			String post = "";
			if (location == null) {
				pre = "default ";
			} else {
				post = String.format(" from [%s]", location);
			}
			throw new TransformerException(
				String.format("Failed to load the %stransformation configuration%s", pre, post), e);
		}
	}

	public static String getDefaultLocation() {
		return Transformer.INSTANCES.getDefaultFileName();
	}

	private final Transformations transformations;
	private final ExternalMetadataLoader metadataLoader;
	private final AttributeMapper attributeMapper;
	private boolean closed = false;

	private Transformer(String location, Transformations transformations, ExternalMetadataLoader metadataLoader,
		AttributeMapper attributeMapper) throws TransformerException {
		this.transformations = transformations;
		this.metadataLoader = metadataLoader;
		this.attributeMapper = attributeMapper;
	}

	private <VALUE> DynamicElementContext<VALUE> createContext(CmfValueMapper mapper, CmfObject<VALUE> object) {
		return new DynamicElementContext<>(object, new DefaultDynamicObject(object), mapper, this.metadataLoader);
	}

	public <VALUE> CmfObject<VALUE> transform(CmfValueMapper mapper, final CmfAttributeNameMapper nameMapper,
		SchemaService schemaService, CmfObject<VALUE> object) throws TransformerException {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (this.closed) { throw new TransformerException("This transformer instance is already closed"); }
			if ((this.transformations == null) && (this.attributeMapper == null)) { return object; }
			DynamicElementContext<VALUE> ctx = createContext(mapper, object);
			try {
				if (this.transformations != null) {
					try {
						this.transformations.apply(ctx);
					} catch (ProcessingCompletedException e) {
						// Do nothing - this is simply our shortcut for stopping the transformation
						// work in its tracks
					}
				}

				final DynamicObject dynamic = ctx.getDynamicObject();
				if (this.attributeMapper != null) {
					try {
						this.attributeMapper.renderMappedAttributes(schemaService, dynamic,
							object.getTranslator().getAttributeNameMapper());
					} catch (SchemaServiceException e) {
						throw new TransformerException(
							String.format("Failed to apply the attribute mappings for %s", object.getDescription()), e);
					}
				}

				return dynamic.applyChanges(object);
			} catch (ActionException e) {
				throw new TransformerException(String
					.format("Exception caught while performing the transformation for %s", object.getDescription()), e);
			} finally {
				destroyContext(ctx);
			}
		}
	}

	private void destroyContext(DynamicElementContext<?> ctx) {
		// Clean things out... to help the GC...
		ctx.getDynamicObject().getAtt().clear();
		ctx.getDynamicObject().getPriv().clear();
		ctx.getVariables().clear();
	}

	public void close() {
		try (MutexAutoLock lock = autoMutexLock()) {
			try {
				if (this.closed) { return; }
				if (this.metadataLoader != null) {
					this.metadataLoader.close();
				}
			} finally {
				this.closed = true;
			}
		}
	}

}