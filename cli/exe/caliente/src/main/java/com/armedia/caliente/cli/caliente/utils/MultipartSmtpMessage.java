package com.armedia.caliente.cli.caliente.utils;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.StringUtils;

public class MultipartSmtpMessage extends SmtpMessage {

	private final Multipart multipart;

	public MultipartSmtpMessage() {
		this(null);
	}

	public MultipartSmtpMessage(String subtype) {
		this.multipart = (StringUtils.isEmpty(subtype) ? new MimeMultipart() : new MimeMultipart(subtype));
	}

	public Multipart getMultipart() {
		return this.multipart;
	}

	@Override
	protected void setContent(Message message) throws MessagingException {
		message.setContent(this.multipart);
	}
}