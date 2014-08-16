/**
 *
 */

package com.delta.cmsmf.io;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.codec.DecoderException;

import com.delta.cmsmf.io.xml.AttributeT;
import com.delta.cmsmf.io.xml.ObjectT;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

/**
 * @author diego
 * 
 */
public class ObjectAttributes {

	private final Map<String, AttributeInfo> attributeInfo;
	private final Map<String, List<Serializable>> values;
	private final String id;
	private final String fileStore;
	private final String type;

	public ObjectAttributes(ObjectT object) {
		if (object == null) { throw new IllegalArgumentException("Must provide an ObjectT to decode"); }
		this.id = object.getId();
		this.type = object.getType();
		this.fileStore = object.getFilestore();
		Map<String, AttributeInfo> attributeInfo = new TreeMap<String, AttributeInfo>();
		Map<String, List<Serializable>> values = new HashMap<String, List<Serializable>>();
		for (AttributeT att : object.getAttribute()) {
			final String name = att.getName();
			attributeInfo.put(name, new AttributeInfo(att));

			final List<String> V = att.getValue();
			final List<Serializable> l = new ArrayList<Serializable>(V.size());
			final AttributeType type = att.getType();
			for (String v : V) {
				l.add(type.getValue(v));
			}
			values.put(name, Collections.unmodifiableList(l));
		}
		this.attributeInfo = Collections.unmodifiableMap(attributeInfo);
		this.values = Collections.unmodifiableMap(values);
	}

	public ObjectAttributes(IDfPersistentObject object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to decode"); }
		Map<String, AttributeInfo> attributeInfo = new TreeMap<String, AttributeInfo>();
		Map<String, List<Serializable>> values = new HashMap<String, List<Serializable>>();
		int attCount = object.getAttrCount();
		for (int a = 0; a < attCount; a++) {
			final IDfAttr attr = object.getAttr(a);
			final String name = attr.getName();
			AttributeType type = AttributeType.decode(attr.getDataType());
			final AttributeInfo info = new AttributeInfo(attr.isRepeating(), type, attr.getLength(), name, attr.getId());
			final int vCount = object.getValueCount(name);
			List<Serializable> list = new ArrayList<Serializable>(vCount);
			for (int v = 0; v < vCount; v++) {
				if (object.isNull(name)) {
					list.add(null);
				} else {
					list.add(type.getValue(object.getValueAt(v)));
				}
			}
			attributeInfo.put(name, info);
			values.put(name, Collections.unmodifiableList(list));
		}
		this.attributeInfo = Collections.unmodifiableMap(attributeInfo);
		this.values = Collections.unmodifiableMap(values);

		this.id = object.getObjectId().getId();
		this.type = object.getType().getName();

		String fileStore = null;
		if (object instanceof IDfSysObject) {
			final IDfSysObject sysObject = IDfSysObject.class.cast(object);
			fileStore = sysObject.getStorageType();
		}
		this.fileStore = fileStore;
	}

	public String getId() {
		return this.id;
	}

	public String getFileStore() {
		return this.fileStore;
	}

	public String getType() {
		return this.type;
	}

	public List<Serializable> getAllValues(String name) {
		return this.values.get(name);
	}

	public Serializable getValue(String name) {
		List<Serializable> v = getAllValues(name);
		if ((v == null) || v.isEmpty()) { return null; }
		return v.get(0);
	}

	public AttributeInfo getAttributeInfo(String name) {
		return this.attributeInfo.get(name);
	}

	public int getAttributeCount() {
		return this.attributeInfo.size();
	}

	public Set<String> getAttributeNames() {
		return this.attributeInfo.keySet();
	}

	public ObjectT getXmlVersion() {
		ObjectT obj = new ObjectT();
		obj.setFilestore(this.fileStore);
		obj.setType(this.type);
		try {
			obj.setId(this.id);
		} catch (DecoderException e) {
			throw new RuntimeException(String.format("Failed to encode the ID [%s] as hex", this.id));
		}

		List<AttributeT> attributes = obj.getAttribute();
		for (final String name : this.attributeInfo.keySet()) {
			final AttributeInfo info = this.attributeInfo.get(name);
			final AttributeT att = info.getXmlVersion();
			attributes.add(att);

			// Now, do the values
			final List<String> values = att.getValue();
			for (final Serializable s : this.values.get(name)) {
				values.add(info.getType().getValue(s));
			}
		}
		return obj;
	}
}