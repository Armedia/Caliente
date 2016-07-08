package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "documentIndexEntry.t", propOrder = {
	"historyId", "count", "versions"
})
public class DocumentIndexEntryT {

	@XmlElement(name = "historyId", required = true)
	protected String historyId;

	@XmlElement(name = "count", required = true)
	protected long count;

	@XmlElement(name = "version", required = true)
	protected List<DocumentIndexVersionT> versions;

	protected void beforeMarshal(Marshaller m) {
		this.count = getVersions().size();
	}

	public String getHistoryId() {
		return this.historyId;
	}

	public void setHistoryId(String value) {
		this.historyId = value;
	}

	public long getCount() {
		return getVersions().size();
	}

	protected final List<DocumentIndexVersionT> getVersions() {
		if (this.versions == null) {
			this.versions = new ArrayList<DocumentIndexVersionT>();
		}
		return this.versions;
	}

	public boolean add(DocumentIndexVersionT v) {
		return getVersions().add(v);
	}

	public boolean add(Collection<DocumentIndexVersionT> v) {
		return getVersions().addAll(v);
	}

	public boolean remove(DocumentIndexVersionT v) {
		return getVersions().remove(v);
	}

	public boolean remove(Collection<DocumentIndexVersionT> v) {
		return getVersions().removeAll(v);
	}

	public boolean contains(DocumentIndexVersionT v) {
		return getVersions().contains(v);
	}

	public boolean contains(Collection<DocumentIndexVersionT> v) {
		return getVersions().containsAll(v);
	}

	public void clear() {
		getVersions().clear();
	}

	@Override
	public String toString() {
		return String.format("DocumentIndexEntryT [historyId=%s, count=%d, versions=%s]", this.historyId, getCount(),
			this.versions);
	}
}