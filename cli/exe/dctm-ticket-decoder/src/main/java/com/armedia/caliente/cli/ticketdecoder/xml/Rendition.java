package com.armedia.caliente.cli.ticketdecoder.xml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rendition.t", propOrder = {
	"pages"
})
public class Rendition {

	@XmlAttribute(name = "type", required = true)
	protected int type;

	@XmlAttribute(name = "format", required = true)
	protected String format;

	@XmlAttribute(name = "modifier", required = true)
	protected String modifier;

	@XmlAttribute(name = "pageCount", required = false)
	protected long pageCount;

	@XmlElement(name = "page", required = true)
	protected List<Page> pages;

	@XmlAttribute(name = "date", required = true)
	protected Date date;

	public int getType() {
		return this.type;
	}

	public Rendition setType(int type) {
		this.type = type;
		return this;
	}

	public String getFormat() {
		return this.format;
	}

	public Rendition setFormat(String format) {
		this.format = format;
		return this;
	}

	public String getModifier() {
		return this.modifier;
	}

	public Rendition setModifier(String modifier) {
		if (StringUtils.isBlank(modifier)) {
			modifier = null;
		}
		this.modifier = modifier;
		return this;
	}

	public long getPageCount() {
		return this.pageCount;
	}

	public Rendition setPageCount(long pageCount) {
		this.pageCount = pageCount;
		return this;
	}

	public Date getDate() {
		return this.date;
	}

	public Rendition setDate(Date date) {
		this.date = date;
		return this;
	}

	public List<Page> getPages() {
		if (this.pages == null) {
			this.pages = new ArrayList<>();
		}
		return this.pages;
	}

	public boolean matches(IDfContent c) throws DfException {
		if (c == null) { return false; }
		if (this.type != c.getRendition()) { return false; }
		if (!Tools.equals(this.format, c.getFullFormat())) { return false; }
		String mod = Tools.coalesce(c.getString("page_modifier"), "");
		if (!Tools.equals(this.modifier, mod)) { return false; }
		return true;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.format, this.pageCount);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Rendition other = Rendition.class.cast(obj);
		if (this.type != other.type) { return false; }
		if (this.pageCount != other.pageCount) { return false; }
		if (!Tools.equals(this.format, other.format)) { return false; }
		if (!Tools.equals(this.modifier, other.modifier)) { return false; }
		if (!Tools.equals(this.pages, other.pages)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Rendition [type=%d, format=%s, modifier=%s, pageCount=%d, pages=%s]", this.type,
			this.format, this.modifier, this.pageCount, this.pages);
	}
}