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