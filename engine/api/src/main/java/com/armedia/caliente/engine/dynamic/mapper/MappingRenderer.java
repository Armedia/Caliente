package com.armedia.caliente.engine.dynamic.mapper;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Triple;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

interface MappingRenderer {

	/**
	 * <p>
	 * Render the mapped values, as a collection of {@link Triple} instances for which the
	 * {@link Triple#getLeft() left} value is the source attribute being mapped, the
	 * {@link Triple#getRight() right} value is the target attribute that the value should be mapped
	 * to, and the {@link Triple#getMiddle() middle} value is the actual rendered value (including
	 * separators, etc).
	 * </p>
	 * <p>
	 * As such, constant mappings have no source attribute, and namespace mappings may return
	 * multiple instances in the collection. An empty or {@code null} collection means nothing was
	 * rendered.
	 * </p>
	 *
	 *
	 * @param object
	 * @param tracker
	 * @return the set of target attributes that were rendered
	 */
	Collection<AttributeValue> render(CmfObject<CmfValue> object, ResidualsModeTracker tracker);

}