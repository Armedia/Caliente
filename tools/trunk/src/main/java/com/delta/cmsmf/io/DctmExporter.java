package com.delta.cmsmf.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

/**
 * This class exists to gather up all of the objects that are intended to be exported. This
 * facilitates the gathering
 * of dependencies and avoiding duplicates, since it becomes the central point from which to carry
 * out those exports.
 * 
 * @author diego
 * 
 */
public class DctmExporter {

	private static class ObjectReference {
		private final String type;
		private final String id;

		ObjectReference(String type, String id) {
			this.type = type;
			this.id = id;
		}
	}

	private final Map<String, Map<String, DctmObject>> dependencies;

	public DctmExporter() {
		this.dependencies = new HashMap<String, Map<String, DctmObject>>();
		for (DctmObjectTypesEnum type : DctmObjectTypesEnum.values()) {
			this.dependencies.put(type.getDocumentumType(), new HashMap<String, DctmObject>());
		}
	}

	private Map<String, DctmObject> getTypeMap(DctmObjectTypesEnum type) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		return getTypeMap(type.getDocumentumType());
	}

	private Map<String, DctmObject> getTypeMap(String type) {
		Map<String, DctmObject> map = null;
		synchronized (this.dependencies) {
			map = this.dependencies.get(type);
			if (map == null) {
				map = new HashMap<String, DctmObject>();
				this.dependencies.put(type, map);
				this.dependencies.notify();
			}
		}
		return map;
	}

	public boolean hasObject(DctmObjectTypesEnum type, String id) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide an object id"); }
		Map<String, DctmObject> map = getTypeMap(type.getDocumentumType());
		synchronized (map) {
			return map.containsKey(id);
		}
	}

	public void addObject(String type, String id) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide an object id"); }
		Map<String, DctmObject> map = getTypeMap(type);
		synchronized (map) {
			if (!map.containsKey(id)) {
				map.put(id, null);
				map.notify();
			}
		}
	}

	public void addObject(DctmObjectTypesEnum type, String id) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide an object id"); }
		Map<String, DctmObject> map = getTypeMap(type);
		synchronized (map) {
			if (!map.containsKey(id)) {
				map.put(id, null);
				map.notify();
			}
		}
	}

	public void addObject(DctmObject obj) {
		if (obj == null) { throw new IllegalArgumentException("Must provide an object to add to the export inventory"); }
		Map<String, DctmObject> map = getTypeMap(obj.dctmObjectType);
		synchronized (map) {
			final String id = obj.getSrcObjectID();
			DctmObject o = map.get(id);
			if (o == null) {
				// We are allowed to overwrite it, since we had only stowed its ID earlier
				map.put(id, obj);
			}
		}
	}

	public void generate(IDfCollection items) throws DfException {
		final IDfSession session = items.getSession();
		List<ObjectReference> mainList = new ArrayList<ObjectReference>();
		while (items.next()) {
			final String id = items.getId("r_object_id").getId();
			final String typeStr = items.getString("r_object_type");
			mainList.add(new ObjectReference(typeStr, id));
		}
		// This is for cleanliness
		items.close();

		for (ObjectReference ref : mainList) {
			resolveDependencies(session, ref.type, ref.id);
		}
	}

	private void storeUser(IDfSession session, String userName) throws DfException {
		IDfUser user = session.getUser(userName);
		if (user == null) { throw new DfException(String.format(String.format(
			"Failed to locate the referenced user [%s]", userName))); }

		// TODO: since we've already pulled it, we should store the actual object
		addObject(DctmObjectTypesEnum.DCTM_USER, user.getObjectId().getId());
	}

	private void storeGroup(IDfSession session, String groupName) throws DfException {
		IDfGroup group = session.getGroup(groupName);
		if (group == null) { throw new DfException(String.format(String.format(
			"Failed to locate the referenced group [%s]", groupName))); }

		// TODO: since we've already pulled it, we should store the actual object
		addObject(DctmObjectTypesEnum.DCTM_GROUP, group.getObjectId().getId());
	}

	private void storeAcl(IDfSession session, String aclDomain, String aclName) throws DfException {
		IDfACL acl = session.getACL(aclDomain, aclName);
		if (acl == null) { throw new DfException(String.format(String.format(
			"Failed to locate the referenced acl [%s]:[%s]", aclDomain, aclName))); }

		// TODO: since we've already pulled it, we should store the actual object
		final int accessors = acl.getAccessorCount();
		for (int i = 0; i < accessors; i++) {
			final String name = acl.getAccessorName(i);
			if (acl.isGroup(i)) {
				storeGroup(session, name);
			} else {
				storeUser(session, name);
			}
		}
		addObject(DctmObjectTypesEnum.DCTM_ACL, acl.getObjectId().getId());
	}

	private void storeFormat(IDfSession session, String formatName) throws DfException {
		IDfFormat format = session.getFormat(formatName);
		if (format == null) { throw new DfException(String.format(String.format(
			"Failed to locate the referenced format [%s]", formatName))); }

		// TODO: since we've already pulled it, we should store the actual object
		addObject(DctmObjectTypesEnum.DCTM_FORMAT, format.getObjectId().getId());
	}

	private void storeCustomType(IDfSession session, String typeName) throws DfException {
		// Custom type, we need to store itself, and its hierarchy
		IDfType type = session.getType(typeName);
		if (type == null) { throw new DfException(String.format(String.format(
			"Failed to locate the referenced type [%s]", typeName))); }

		String superType = type.getSuperName();
		if ((superType != null) && (DctmObjectTypesEnum.decode(superType) == null)) {
			// Traverse the hierarchy upwards
			storeCustomType(session, superType);
		}

		// TODO: Find the type_info data, to store any ACL info
		// TODO: since we've already pulled it, we should store the actual object
		addObject(DctmObjectTypesEnum.DCTM_TYPE, type.getObjectId().getId());
	}

	private void storeFolder(IDfSession session, String folderId) throws DfException {
		// Custom type, we need to store itself, and its hierarchy
		IDfFolder folder = session.getFolderBySpecification(folderId);
		if (folder == null) { throw new DfException(String.format(String.format(
			"Failed to locate the referenced folder [%s]", folderId))); }

		storeUser(session, folder.getOwnerName());
		storeGroup(session, folder.getGroupName());
		storeFormat(session, folder.getFormat().getName());
		storeAcl(session, folder.getACLDomain(), folder.getACLName());

		// TODO: since we've already pulled it, we should store the actual object
		addObject(DctmObjectTypesEnum.DCTM_FOLDER, folderId);
	}

	private void storeDocument(IDfSession session, String objectId) throws DfException {
		// Custom type, we need to store itself, and its hierarchy
		IDfPersistentObject obj = session.getObjectByQualification(String.format(
			"dm_document where r_object_id = '%s'", objectId));
		if (obj == null) { throw new DfException(String.format(String.format(
			"Failed to locate the referenced document [%s]", objectId))); }
		IDfDocument document = IDfDocument.class.cast(obj);

		storeUser(session, document.getOwnerName());
		storeGroup(session, document.getGroupName());
		storeFormat(session, document.getFormat().getName());
		storeAcl(session, document.getACLDomain(), document.getACLName());

		// TODO: since we've already pulled it, we should store the actual object
		addObject(DctmObjectTypesEnum.DCTM_FOLDER, objectId);
	}

	private void storeContent(IDfSession session, String objectId) throws DfException {

	}

	private void resolveDependencies(IDfSession session, String type, String id) throws DfException {
		IDfPersistentObject obj = session.getObject(new DfId(id));
		if (obj == null) { throw new DfException(String.format(String.format(
			"Failed to locate the referenced object [%s]", id))); }

		// Read all the attributes in
		ObjectAttributes attributes = new ObjectAttributes(obj);

		// Go for the dependencies
		// * Owner
		String ownerAtt = "owner_name"; // TODO: Get this from the actual type
		if ((ownerAtt != null) && obj.hasAttr(ownerAtt)) {
			for (int i = 0; i < obj.getValueCount(ownerAtt); i++) {
				storeUser(session, obj.getRepeatingString(ownerAtt, i));
			}
		}

		// * Group
		String groupAtt = "group_name"; // TODO: Get this from the actual type
		if ((groupAtt != null) && obj.hasAttr(groupAtt)) {
			for (int i = 0; i < obj.getValueCount(groupAtt); i++) {
				storeGroup(session, obj.getRepeatingString(groupAtt, i));
			}
		}

		// * ACL
		String aclAtt = "acl_name"; // TODO: Get this from the actual type
		String aclDom = "acl_domain"; // TODO: Get this from the actual type
		if ((aclAtt != null) && obj.hasAttr(aclAtt) && (aclDom != null) && obj.hasAttr(aclDom)) {
			for (int i = 0; i < obj.getValueCount(aclAtt); i++) {
				storeAcl(session, obj.getRepeatingString(aclDom, i), obj.getRepeatingString(aclAtt, i));
			}
		}

		// * Type (already have it)
		DctmObjectTypesEnum t = DctmObjectTypesEnum.decode(type);
		if (t == null) {
			storeCustomType(session, type);
		}

		// * Format
		String formatAtt = "format"; // TODO: Get this from the actual type
		if ((formatAtt != null) && obj.hasAttr(formatAtt)) {
			for (int i = 0; i < obj.getValueCount(formatAtt); i++) {
				storeFormat(session, obj.getRepeatingString(formatAtt, i));
			}
		}

		// * Containing Folders
		String folderAtt = "i_folder_id"; // TODO: Get this from actual type
		if ((folderAtt != null) && obj.hasAttr(folderAtt)) {
			for (int i = 0; i < obj.getValueCount(folderAtt); i++) {
				storeFolder(session, obj.getRepeatingString(folderAtt, i));
			}
		}

		// * Versions
		// * Renditions
	}

	public void write(File rootDir) throws IOException {

	}

	public void read(File rootDir) throws IOException {

	}
}