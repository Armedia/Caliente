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
package com.armedia.caliente.engine.tools.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObject.Archetype;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValue.Type;

public class MetadataTTest {

	private static final CmfObject<CmfValue> OBJECT;

	private static CmfValue generateValue(int count, Random r, Type type) throws Exception {
		Object value = null;
		switch (type) {
			case BOOLEAN:
				value = r.nextBoolean();
				break;
			case DATETIME:
				value = new Date();
				break;
			case DOUBLE:
				value = r.nextDouble();
				break;
			case HTML:
				value = "some-HTML-value";
				break;
			case ID:
				value = "some-ID-value";
				break;
			case INTEGER:
				value = r.nextInt(1000);
				break;
			case STRING:
				value = UUID.randomUUID().toString();
				break;
			case URI:
				value = new URI("scheme", "ssp", "fragment");
				break;
			case BASE64_BINARY:
				byte[] arr = new byte[128];
				r.nextBytes(arr);
				value = arr;
				break;
			case OTHER:
				return null;
		}
		return new CmfValue(type, value);
	}

	static {
		Random r = new Random(System.nanoTime());
		Collection<CmfObjectRef> parents = null;
		for (int i = 0; i < (r.nextInt(10) + 3); i++) {
			if (parents == null) {
				parents = new LinkedList<>();
			}
			parents.add(new CmfObjectRef(Archetype.FOLDER, String.format("PARENT-%02d", i)));
		}
		Set<String> secondarySubtypes = null;
		for (int i = 0; i < (r.nextInt(10) + 5); i++) {
			if (secondarySubtypes == null) {
				secondarySubtypes = new LinkedHashSet<>();
			}
			secondarySubtypes.add(String.format("SUBTYPE-%02d", i));
		}
		OBJECT = new CmfObject<>(CmfAttributeTranslator.CMFVALUE_TRANSLATOR, Archetype.DOCUMENT, "OBJECT-ID",
			"OBJECT-NAME", parents, "OBJECT-SEARCH-KEY", 0, "HISTORY-ID", false, "OBJECT-LABEL", "OBJECT-SUBTYPE",
			secondarySubtypes, 123L);
		for (int i = 0; i < (r.nextInt(10) + 10); i++) {
			String attName = String.format("attribute.%02d", i);
			Type type = Type.values()[i % Type.values().length];
			if (type == Type.OTHER) {
				continue;
			}
			boolean multivalue = ((i % 2) == 0);
			int valueCount = (multivalue ? r.nextInt(10) : 1);
			Collection<CmfValue> values = new ArrayList<>(valueCount);
			for (int v = 0; v < 5; v++) {
				try {
					values.add(MetadataTTest.generateValue(i, r, type));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			MetadataTTest.OBJECT.setAttribute(new CmfAttribute<>(attName, type, multivalue, values));
		}
		for (int i = 0; i < (r.nextInt(10) + 10); i++) {
			String propName = String.format("property.%02d", i);
			Type type = Type.values()[i % Type.values().length];
			if (type == Type.OTHER) {
				continue;
			}
			boolean multivalue = ((i % 2) == 0);
			int valueCount = (multivalue ? r.nextInt(10) : 1);
			Collection<CmfValue> values = new ArrayList<>(valueCount);
			for (int v = 0; v < 5; v++) {
				try {
					values.add(MetadataTTest.generateValue(i, r, type));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			MetadataTTest.OBJECT.setProperty(new CmfProperty<>(propName, type, multivalue, values));
		}
	}

	@Test
	public void testMarshalling() throws Exception {

		StringWriter sw = new StringWriter();

		XmlBase.storeToXML(new MetadataT(MetadataTTest.OBJECT), sw, true);
		sw.flush();
		System.out.print(sw);

		MetadataT metadata = XmlBase.loadFromXML(MetadataT.class, new StringReader(sw.toString()));
		CmfObject<CmfValue> o = metadata.getObject();
		Assertions.assertSame(MetadataTTest.OBJECT.getType(), o.getType());
		Assertions.assertEquals(MetadataTTest.OBJECT.getId(), o.getId());
		Assertions.assertEquals(MetadataTTest.OBJECT.getSearchKey(), o.getSearchKey());
		Assertions.assertEquals(MetadataTTest.OBJECT.getNumber(), o.getNumber());
		Assertions.assertEquals(MetadataTTest.OBJECT.getName(), o.getName());
		Assertions.assertEquals(MetadataTTest.OBJECT.getLabel(), o.getLabel());
		Collection<CmfObjectRef> parentsA = MetadataTTest.OBJECT.getParentReferences();
		Assertions.assertNotNull(parentsA);
		Collection<CmfObjectRef> parentsB = o.getParentReferences();
		Assertions.assertNotNull(parentsB);
		Assertions.assertEquals(parentsA.size(), parentsB.size());
		Iterator<CmfObjectRef> itA = parentsA.iterator();
		Iterator<CmfObjectRef> itB = parentsB.iterator();
		while (itA.hasNext() && itB.hasNext()) {
			CmfObjectRef a = itA.next();
			CmfObjectRef b = itB.next();
			Assertions.assertEquals(a, b);
		}
		Assertions.assertEquals(itA.hasNext(), itB.hasNext());

		Assertions.assertEquals(MetadataTTest.OBJECT.getDependencyTier(), o.getDependencyTier());
		Assertions.assertEquals(MetadataTTest.OBJECT.getHistoryId(), o.getHistoryId());
		Assertions.assertEquals(MetadataTTest.OBJECT.isHistoryCurrent(), o.isHistoryCurrent());
		Assertions.assertEquals(MetadataTTest.OBJECT.getSubtype(), o.getSubtype());
		Assertions.assertEquals(MetadataTTest.OBJECT.getSecondarySubtypes(), o.getSecondarySubtypes());
		Assertions.assertEquals(MetadataTTest.OBJECT.getAttributeCount(), o.getAttributeCount());
		Assertions.assertEquals(MetadataTTest.OBJECT.getAttributeNames(), o.getAttributeNames());
		for (String att : MetadataTTest.OBJECT.getAttributeNames()) {
			CmfAttribute<CmfValue> a = MetadataTTest.OBJECT.getAttribute(att);
			Assertions.assertNotNull(a);
			CmfAttribute<CmfValue> b = MetadataTTest.OBJECT.getAttribute(att);
			Assertions.assertNotNull(b);
			Assertions.assertSame(a.getType(), b.getType());
			Assertions.assertEquals(a.getValueCount(), b.getValueCount());
			for (int i = 0; i < a.getValueCount(); i++) {
				Assertions.assertEquals(a.getValue(i), b.getValue(i));
			}
		}
		for (String prop : MetadataTTest.OBJECT.getPropertyNames()) {
			CmfProperty<CmfValue> a = MetadataTTest.OBJECT.getProperty(prop);
			Assertions.assertNotNull(a);
			CmfProperty<CmfValue> b = MetadataTTest.OBJECT.getProperty(prop);
			Assertions.assertNotNull(b);
			Assertions.assertSame(a.getType(), b.getType());
			Assertions.assertEquals(a.getValueCount(), b.getValueCount());
			for (int i = 0; i < a.getValueCount(); i++) {
				Assertions.assertEquals(a.getValue(i), b.getValue(i));
			}
		}
	}

}