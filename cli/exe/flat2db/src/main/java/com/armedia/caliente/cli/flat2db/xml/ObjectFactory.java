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
// Implementation, vhudson-jaxb-ri-2.2-147
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.01 at 11:04:18 AM CST
//

package com.armedia.caliente.cli.flat2db.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface
 * generated in the com.armedia.caliente.cli.flat2db.xml package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation
 * for XML content. The Java representation of XML content can consist of schema derived interfaces
 * and classes representing the binding of schema type definitions, element declarations and model
 * groups. Factory methods for each of these are provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {
	public static final String NAMESPACE = "http://www.armedia.com/ns/caliente/flat2db";

	private final static QName _SqlTasksTSql_QNAME = new QName(ObjectFactory.NAMESPACE, "sql");
	private final static QName _SqlTasksTSqlScript_QNAME = new QName(ObjectFactory.NAMESPACE, "sql-script");
	private final static QName _SqlInitializerTasksTRollbackChangeset_QNAME = new QName(ObjectFactory.NAMESPACE,
		"rollback-changeset");
	private final static QName _SqlInitializerTasksTChangeset_QNAME = new QName(ObjectFactory.NAMESPACE, "changeset");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.caliente.cli.flat2db.xml
	 *
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link SqlTasksT }
	 *
	 */
	public SqlTasksT createSqlTasksT() {
		return new SqlTasksT();
	}

	/**
	 * Create an instance of {@link StructureFilterT }
	 *
	 */
	public StructureFilterT createStructureFilterT() {
		return new StructureFilterT();
	}

	/**
	 * Create an instance of {@link PropertiesT }
	 *
	 */
	public PropertiesT createPropertiesT() {
		return new PropertiesT();
	}

	/**
	 * Create an instance of {@link StructuresT }
	 *
	 */
	public StructuresT createStructuresT() {
		return new StructuresT();
	}

	/**
	 * Create an instance of {@link SheetStructureT }
	 *
	 */
	public SheetStructureT createSheetStructureT() {
		return new SheetStructureT();
	}

	/**
	 * Create an instance of {@link SqlInitializerTasksT }
	 *
	 */
	public SqlInitializerTasksT createSqlInitializerTasksT() {
		return new SqlInitializerTasksT();
	}

	/**
	 * Create an instance of {@link StructureT }
	 *
	 */
	public StructureT createStructureT() {
		return new StructureT();
	}

	/**
	 * Create an instance of {@link StructureHeaderT }
	 *
	 */
	public StructureHeaderT createStructureHeaderT() {
		return new StructureHeaderT();
	}

	/**
	 * Create an instance of {@link SheetStructureDefaultT }
	 *
	 */
	public SheetStructureDefaultT createSheetStructureDefaultT() {
		return new SheetStructureDefaultT();
	}

	/**
	 * Create an instance of {@link StructureColumnT }
	 *
	 */
	public StructureColumnT createStructureColumnT() {
		return new StructureColumnT();
	}

	/**
	 * Create an instance of {@link StructureRangeT }
	 *
	 */
	public StructureRangeT createStructureRangeT() {
		return new StructureRangeT();
	}

	/**
	 * Create an instance of {@link DataSourceT }
	 *
	 */
	public DataSourceT createDataSourceT() {
		return new DataSourceT();
	}

	/**
	 * Create an instance of {@link RollbackChangesetT }
	 *
	 */
	public RollbackChangesetT createRollbackChangesetT() {
		return new RollbackChangesetT();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "sql", scope = SqlTasksT.class)
	public JAXBElement<String> createSqlTasksTSql(String value) {
		return new JAXBElement<>(ObjectFactory._SqlTasksTSql_QNAME, String.class, SqlTasksT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "sql-script", scope = SqlTasksT.class)
	public JAXBElement<String> createSqlTasksTSqlScript(String value) {
		return new JAXBElement<>(ObjectFactory._SqlTasksTSqlScript_QNAME, String.class, SqlTasksT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link RollbackChangesetT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "rollback-changeset", scope = SqlInitializerTasksT.class)
	public JAXBElement<RollbackChangesetT> createSqlInitializerTasksTRollbackChangeset(RollbackChangesetT value) {
		return new JAXBElement<>(ObjectFactory._SqlInitializerTasksTRollbackChangeset_QNAME, RollbackChangesetT.class,
			SqlInitializerTasksT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "changeset", scope = SqlInitializerTasksT.class)
	public JAXBElement<String> createSqlInitializerTasksTChangeset(String value) {
		return new JAXBElement<>(ObjectFactory._SqlInitializerTasksTChangeset_QNAME, String.class,
			SqlInitializerTasksT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "sql", scope = SqlInitializerTasksT.class)
	public JAXBElement<String> createSqlInitializerTasksTSql(String value) {
		return new JAXBElement<>(ObjectFactory._SqlTasksTSql_QNAME, String.class, SqlInitializerTasksT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "sql-script", scope = SqlInitializerTasksT.class)
	public JAXBElement<String> createSqlInitializerTasksTSqlScript(String value) {
		return new JAXBElement<>(ObjectFactory._SqlTasksTSqlScript_QNAME, String.class, SqlInitializerTasksT.class,
			value);
	}

}
