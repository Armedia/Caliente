/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2015.11.19 at 03:26:27 PM CST
//

package com.armedia.caliente.engine.xml.importer.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface
 * generated in the com.armedia.caliente.engine.xml.importer.jaxb package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation
 * for XML content. The Java representation of XML content can consist of schema derived interfaces
 * and classes representing the binding of schema type definitions, element declarations and model
 * groups. Factory methods for each of these are provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

	static final String NAMESPACE = "http://www.armedia.com/ns/caliente/engine/xml";

	private final static QName _DocumentIndex_QNAME = new QName(ObjectFactory.NAMESPACE, "documentIndex");
	private final static QName _Types_QNAME = new QName(ObjectFactory.NAMESPACE, "types");
	private final static QName _Groups_QNAME = new QName(ObjectFactory.NAMESPACE, "groups");
	private final static QName _Folder_QNAME = new QName(ObjectFactory.NAMESPACE, "folder");
	private final static QName _Users_QNAME = new QName(ObjectFactory.NAMESPACE, "users");
	private final static QName _Document_QNAME = new QName(ObjectFactory.NAMESPACE, "document");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.caliente.engine.xml.importer.jaxb
	 *
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link DocumentIndexEntryT }
	 *
	 */
	public DocumentIndexEntryT createDocumentIndexEntryT() {
		return new DocumentIndexEntryT();
	}

	/**
	 * Create an instance of {@link TypeT }
	 *
	 */
	public TypeT createTypeDefT() {
		return new TypeT();
	}

	/**
	 * Create an instance of {@link AttributeDefT }
	 *
	 */
	public AttributeDefT createAttributeDefT() {
		return new AttributeDefT();
	}

	/**
	 * Create an instance of {@link DocumentVersionT }
	 *
	 */
	public DocumentVersionT createDocumentVersionT() {
		return new DocumentVersionT();
	}

	/**
	 * Create an instance of {@link GroupsT }
	 *
	 */
	public GroupsT createGroupsT() {
		return new GroupsT();
	}

	/**
	 * Create an instance of {@link AttributeBaseT }
	 *
	 */
	public AttributeBaseT createAttributeBaseT() {
		return new AttributeBaseT();
	}

	/**
	 * Create an instance of {@link UsersT }
	 *
	 */
	public UsersT createUsersT() {
		return new UsersT();
	}

	/**
	 * Create an instance of {@link AttributeT }
	 *
	 */
	public AttributeT createAttributeT() {
		return new AttributeT();
	}

	/**
	 * Create an instance of {@link TypesT }
	 *
	 */
	public TypesT createTypesT() {
		return new TypesT();
	}

	/**
	 * Create an instance of {@link DocumentT }
	 *
	 */
	public DocumentT createDocumentT() {
		return new DocumentT();
	}

	/**
	 * Create an instance of {@link UserT }
	 *
	 */
	public UserT createUserT() {
		return new UserT();
	}

	/**
	 * Create an instance of {@link SysObjectT }
	 *
	 */
	public SysObjectT createSysObjectT() {
		return new SysObjectT();
	}

	/**
	 * Create an instance of {@link FolderT }
	 *
	 */
	public FolderT createFolderT() {
		return new FolderT();
	}

	/**
	 * Create an instance of {@link GroupT }
	 *
	 */
	public GroupT createGroupT() {
		return new GroupT();
	}

	/**
	 * Create an instance of {@link DocumentIndexT }
	 *
	 */
	public DocumentIndexT createDocumentIndexT() {
		return new DocumentIndexT();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link DocumentIndexT }{@code >}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "documentIndex")
	public JAXBElement<DocumentIndexT> createDocumentIndex(DocumentIndexT value) {
		return new JAXBElement<>(ObjectFactory._DocumentIndex_QNAME, DocumentIndexT.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link TypesT }{@code >}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "types")
	public JAXBElement<TypesT> createTypes(TypesT value) {
		return new JAXBElement<>(ObjectFactory._Types_QNAME, TypesT.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link GroupsT }{@code >}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "groups")
	public JAXBElement<GroupsT> createGroups(GroupsT value) {
		return new JAXBElement<>(ObjectFactory._Groups_QNAME, GroupsT.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FolderT }{@code >}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "folder")
	public JAXBElement<FolderT> createFolder(FolderT value) {
		return new JAXBElement<>(ObjectFactory._Folder_QNAME, FolderT.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link UsersT }{@code >}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "users")
	public JAXBElement<UsersT> createUsers(UsersT value) {
		return new JAXBElement<>(ObjectFactory._Users_QNAME, UsersT.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link DocumentT }{@code >}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "document")
	public JAXBElement<DocumentT> createDocument(DocumentT value) {
		return new JAXBElement<>(ObjectFactory._Document_QNAME, DocumentT.class, null, value);
	}

}
