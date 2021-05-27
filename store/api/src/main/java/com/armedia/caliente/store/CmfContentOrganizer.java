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
package com.armedia.caliente.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.LazySupplier;

public abstract class CmfContentOrganizer {

	private static final Logger LOG = LoggerFactory.getLogger(CmfContentOrganizer.class);

	private static final String ORGANIZER_PREFIX = "organizer.";

	private static CfgTools getOrganizerSettings(CfgTools settings) {
		Set<String> names = new LinkedHashSet<>(settings.getSettings());
		names.removeIf((n) -> !StringUtils.startsWith(n, CmfContentOrganizer.ORGANIZER_PREFIX));
		Map<String, Object> values = new LinkedHashMap<>(names.size());
		for (String n : names) {
			List<Object> v = settings.getObjects(n);
			n = n.substring(CmfContentOrganizer.ORGANIZER_PREFIX.length());
			if (!StringUtils.isEmpty(n) && (v != null) && !v.isEmpty()) {
				values.put(n, v);
			}
		}
		return new CfgTools(values);
	}

	public static final class Location implements Comparable<Location>, Serializable {
		private static final long serialVersionUID = 1L;

		public final List<String> containerSpec;
		public final String baseName;
		public final String extension;
		public final String descriptor;
		public final String appendix;
		private final LazySupplier<String> string;

		private Location(List<String> containerSpec, String baseName, String extension, String descriptor,
			String appendix) {
			this.containerSpec = Tools.freezeCopy(containerSpec, true);
			this.baseName = baseName;
			this.extension = extension;
			this.descriptor = descriptor;
			this.appendix = appendix;
			this.string = new LazySupplier<>(this::renderString);
		}

		@Override
		public int compareTo(Location o) {
			if (o == null) { return 1; }
			int r = 0;
			r = Tools.compare(this.containerSpec.size(), o.containerSpec.size());
			if (r != 0) { return r; }

			for (int i = 0; i < this.containerSpec.size(); i++) {
				r = Tools.compare(this.containerSpec.get(i), o.containerSpec.get(i));
				if (r != 0) { return r; }
			}

			r = Tools.compare(this.baseName, o.baseName);
			if (r != 0) { return r; }
			r = Tools.compare(this.extension, o.extension);
			if (r != 0) { return r; }
			r = Tools.compare(this.descriptor, o.descriptor);
			if (r != 0) { return r; }
			r = Tools.compare(this.appendix, o.appendix);
			if (r != 0) { return r; }
			return 0;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.containerSpec, this.baseName, this.extension, this.descriptor,
				this.appendix);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			Location other = Location.class.cast(obj);
			if (!Objects.equals(this.containerSpec, other.containerSpec)) { return false; }
			if (!Objects.equals(this.baseName, other.baseName)) { return false; }
			if (!Objects.equals(this.extension, other.extension)) { return false; }
			if (!Objects.equals(this.descriptor, other.descriptor)) { return false; }
			if (!Objects.equals(this.appendix, other.appendix)) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return this.string.get();
		}

		private String renderString() {
			final String containerSpec;
			StringBuilder b = new StringBuilder();
			for (String s : this.containerSpec) {
				if (b.length() > 0) {
					b.append('/');
				}
				b.append(s);
			}
			containerSpec = b.toString();
			final String baseName = (!StringUtils.isEmpty(this.baseName) ? this.baseName : "");
			final String descriptor = (!StringUtils.isEmpty(this.descriptor) ? String.format("[%s]", this.descriptor)
				: "");
			final String extension = (!StringUtils.isEmpty(this.extension) ? String.format(".%s", this.extension) : "");
			final String appendix = (!StringUtils.isEmpty(this.appendix) ? String.format(".%s", this.appendix) : "");
			return String.format("%s/%s%s%s%s", containerSpec, baseName, descriptor, extension, appendix);
		}
	}

	private static final CmfContentOrganizer DEFAULT_ORGANIZER = new CmfContentOrganizer() {

		@Override
		public <T> Location calculateLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
			CmfContentStream info) {
			List<String> path = new ArrayList<>(3);
			path.add(object.getType().name());
			path.add(object.getSubtype());
			return newLocation(path, object.getId(), null, null, null);
		}
	};

	private static final Pattern VALIDATOR = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");

	private static final boolean isValidName(String name) {
		return (name != null) && CmfContentOrganizer.VALIDATOR.matcher(name).matches();
	}

	private static final Map<String, CmfContentOrganizer> ORGANIZERS;

	static {
		Map<String, CmfContentOrganizer> strategies = new HashMap<>();
		PluggableServiceLocator<CmfContentOrganizer> l = new PluggableServiceLocator<>(CmfContentOrganizer.class);
		l.setHideErrors(false);
		l.setErrorListener((serviceClass, t) -> {
			if (CmfContentOrganizer.LOG.isDebugEnabled()) {
				CmfContentOrganizer.LOG.warn("Failed to instantiate the content organizer class [{}]",
					serviceClass.getCanonicalName(), t);
			}
		});
		for (CmfContentOrganizer s : l) {
			String name = s.getName();
			if (name == null) {
				CmfContentOrganizer.LOG.warn(
					"The content organizer class [{}] did not provide a name, so it won't be registered",
					s.getClass().getCanonicalName());
				continue;
			}
			CmfContentOrganizer old = strategies.get(name);
			if (old != null) {
				CmfContentOrganizer.LOG.warn(
					"The content organizer class [{}] provides the name [{}], but this collides with already-registered organizer class [{}]. The newcomer will be ignored.",
					s.getClass().getCanonicalName(), name, old.getClass().getCanonicalName());
				continue;
			}
			CmfContentOrganizer.LOG.debug("Registering the content organizer class [{}] as [{}]",
				s.getClass().getCanonicalName(), name);
			strategies.put(name, s);
		}
		ORGANIZERS = Tools.freezeMap(strategies);
	}

	public static CmfContentOrganizer getOrganizer(String name) {
		if (name == null) { return CmfContentOrganizer.DEFAULT_ORGANIZER; }
		return Tools.coalesce(CmfContentOrganizer.ORGANIZERS.get(name), CmfContentOrganizer.DEFAULT_ORGANIZER);
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final String name;

	CmfContentOrganizer() {
		this.name = null;
	}

	protected CmfContentOrganizer(String name) {
		if (!CmfContentOrganizer.isValidName(name)) {
			throw new IllegalArgumentException(
				String.format("The string [%s] is not valid for a content organizer name", name));
		}
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}

	protected abstract <T> Location calculateLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info);

	public final <T> Location getLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		if (translator == null) { throw new IllegalArgumentException("Must provide an attribute translator"); }
		if (object == null) { throw new IllegalArgumentException("Must provide a CMF object"); }
		if (info == null) { throw new IllegalArgumentException("Must provide a valid Content Information object"); }
		return calculateLocation(translator, object, info);
	}

	public final void configure(CfgTools settings) {
		doConfigure(CmfContentOrganizer.getOrganizerSettings(settings));
	}

	protected void doConfigure(CfgTools settings) {
		// By default, do nothing
	}

	protected final Location newLocation(List<String> containerSpec, String baseName, String extension,
		String descriptor, String appendix) {
		return new Location(containerSpec, baseName, extension, descriptor, appendix);
	}
}