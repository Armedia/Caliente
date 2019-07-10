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
package com.armedia.caliente.cli.caliente.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;

public abstract class SmtpMessage {

	private static final InternetAddress[] NO_ADDRESS = new InternetAddress[0];

	private String from = null;

	private final Set<String> to = new LinkedHashSet<>();
	private final Set<String> cc = new LinkedHashSet<>();
	private final Set<String> bcc = new LinkedHashSet<>();

	private String subject = null;

	public String getFrom() {
		return this.from;
	}

	public SmtpMessage setFrom(String from) {
		this.from = from;
		return this;
	}

	public String getSubject() {
		return this.subject;
	}

	public SmtpMessage setSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public Set<String> getTo() {
		return this.to;
	}

	public Set<String> getCc() {
		return this.cc;
	}

	public Set<String> getBcc() {
		return this.bcc;
	}

	public final Message build(Session session) throws MessagingException {
		Objects.requireNonNull(session, "Must provide a non-null Session object");
		Message msg = new MimeMessage(session);

		// set the from and to addresses
		InternetAddress addressFrom = new InternetAddress(this.from);
		msg.setFrom(addressFrom);

		int recipientCount = 0;
		Collection<InternetAddress> recipients = new ArrayList<>();
		for (String recipient : this.to) {
			if (!StringUtils.isEmpty(recipient)) {
				recipients.add(new InternetAddress(recipient));
				recipientCount++;
			}
		}
		if (!recipients.isEmpty()) {
			msg.setRecipients(Message.RecipientType.TO, recipients.toArray(SmtpMessage.NO_ADDRESS));
		}

		recipients = new ArrayList<>();
		for (String recipient : this.cc) {
			if (!StringUtils.isEmpty(recipient)) {
				recipients.add(new InternetAddress(recipient));
				recipientCount++;
			}
		}
		if (!recipients.isEmpty()) {
			msg.setRecipients(Message.RecipientType.CC, recipients.toArray(SmtpMessage.NO_ADDRESS));
		}

		recipients = new ArrayList<>();
		for (String recipient : this.bcc) {
			if (!StringUtils.isEmpty(recipient)) {
				recipients.add(new InternetAddress(recipient));
				recipientCount++;
			}
		}
		if (!recipients.isEmpty()) {
			msg.setRecipients(Message.RecipientType.BCC, recipients.toArray(SmtpMessage.NO_ADDRESS));
		}

		if (recipientCount == 0) { throw new MessagingException("No TO, CC or BCC addresses given"); }

		msg.setSubject(this.subject);
		setContent(msg);
		return msg;
	}

	protected abstract void setContent(Message message) throws MessagingException;
}