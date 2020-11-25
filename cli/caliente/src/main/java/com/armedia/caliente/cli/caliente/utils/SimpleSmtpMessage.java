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
package com.armedia.caliente.cli.caliente.utils;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class SimpleSmtpMessage extends SmtpMessage {

	public static final MimeType DEFAULT_MIME_TYPE;
	static {
		try {
			DEFAULT_MIME_TYPE = new MimeType("text/plain");
		} catch (MimeTypeParseException e) {
			throw new RuntimeException("Failed to initialize the default mime type application/octet-stream");
		}
	}

	private MimeType mimeType = SimpleSmtpMessage.DEFAULT_MIME_TYPE;
	private String message = null;

	public String getMessage() {
		return this.message;
	}

	public SimpleSmtpMessage setMimeType(MimeType mimeType) {
		this.mimeType = Tools.coalesce(mimeType, SimpleSmtpMessage.DEFAULT_MIME_TYPE);
		return this;
	}

	public MimeType getMimeType() {
		return this.mimeType;
	}

	public SimpleSmtpMessage setMessage(String message) {
		this.message = message;
		return this;
	}

	@Override
	protected void setContent(Message message) throws MessagingException {
		message.setContent(Tools.coalesce(message, StringUtils.EMPTY), Tools.toString(this.mimeType));
	}
}