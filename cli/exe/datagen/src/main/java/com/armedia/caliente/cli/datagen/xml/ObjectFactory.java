package com.armedia.caliente.cli.datagen.xml;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

	static final String NS = "http://www.armedia.com/ns/data-generator";

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.cmf.generator2
	 *
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link GenerationPlan }
	 *
	 */
	public GenerationPlan createDataT() {
		return new GenerationPlan();
	}

	/**
	 * Create an instance of {@link FsObject }
	 *
	 */
	public FsObject createFsObjectT() {
		return new FsObject();
	}

	/**
	 * Create an instance of {@link Folder }
	 *
	 */
	public Folder createFolderT() {
		return new Folder();
	}

	/**
	 * Create an instance of {@link Value }
	 *
	 */
	public Value createValueT() {
		return new Value();
	}

	/**
	 * Create an instance of {@link Attribute }
	 *
	 */
	public Attribute createAttributeT() {
		return new Attribute();
	}

	/**
	 * Create an instance of {@link GeneratedContent }
	 *
	 */
	public GeneratedContent createGeneratedContentT() {
		return new GeneratedContent();
	}

	/**
	 * Create an instance of {@link Rendition }
	 *
	 */
	public Rendition createRenditionT() {
		return new Rendition();
	}

	/**
	 * Create an instance of {@link Document }
	 *
	 */
	public Document createDocumentT() {
		return new Document();
	}

	/**
	 * Create an instance of {@link UrlContent }
	 *
	 */
	public UrlContent createUrlContentT() {
		return new UrlContent();
	}

	/**
	 * Create an instance of {@link Content }
	 *
	 */
	public Content createContentT() {
		return new Content();
	}

	/**
	 * Create an instance of {@link Page }
	 *
	 */
	public Page createPageT() {
		return new Page();
	}
}