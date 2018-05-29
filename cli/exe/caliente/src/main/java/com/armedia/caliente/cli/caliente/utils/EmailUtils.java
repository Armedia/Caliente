package com.armedia.caliente.cli.caliente.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.caliente.cfg.Setting;

public class EmailUtils {

	/** The log object used for logging. */
	private static Logger log = LoggerFactory.getLogger(EmailUtils.class);

	private static final InternetAddress[] NO_ADDRESS = new InternetAddress[0];

	/**
	 * Post mail.
	 *
	 * @param recipients
	 *            the recipients
	 * @param subject
	 *            the subject
	 * @param message
	 *            the message
	 * @param from
	 *            the from
	 * @throws MessagingException
	 *             the messaging exception
	 */
	public static void postMail(String smtpHost, Collection<String> recipients, String subject, String message,
		String from) throws MessagingException {
		boolean debug = false;

		// Set the host smtp address
		Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);

		// TODO: Support authenticated SMTP
		Authenticator auth = null;

		// Get the default Session
		Session session = Session.getDefaultInstance(props, auth);
		session.setDebug(debug);

		// create a message
		Message msg = new MimeMessage(session);

		// set the from and to addresses
		InternetAddress addressFrom = new InternetAddress(from);
		msg.setFrom(addressFrom);

		Collection<InternetAddress> addressTo = new ArrayList<>(recipients.size());
		for (String recipient : recipients) {
			if (!StringUtils.isEmpty(recipient)) {
				addressTo.add(new InternetAddress(recipient));
			}
		}
		if (addressTo.isEmpty()) { throw new MessagingException("No destination addresses given"); }
		msg.setRecipients(Message.RecipientType.TO, addressTo.toArray(EmailUtils.NO_ADDRESS));

		// Optional : You can also set your custom headers in the Email if you Want
		msg.addHeader("X-CalienteHeader", "Caliente-Related");

		// Setting the Subject and Content Type
		msg.setSubject(subject);
		msg.setContent(message, "text/plain");
		Transport.send(msg);
	}

	/**
	 * Post Caliente mail.
	 *
	 * @param subject
	 *            the subject of the email message
	 * @param message
	 *            the body of the email
	 * @throws MessagingException
	 *             the messaging exception
	 */
	public static void postCalienteMail(String subject, String message) throws MessagingException {

		String mailRecipients = Setting.MAIL_RECIPIENTS.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(mailRecipients);
		Collection<String> recipients = strTokenizer.getTokenList();

		if (recipients.isEmpty()) {
			EmailUtils.log.warn("No recipients found for submitting the e-mail.");
			return;
		}

		String mailFromAddress = Setting.MAIL_FROM_ADDX.getString();
		if (StringUtils.isBlank(mailFromAddress)) {
			EmailUtils.log.warn(String.format(
				"No FROM address for the e-mail, the intended recipients (%s) won't receive an e-mail.", recipients));
			return;
		}

		String smtpHostAddress = Setting.MAIL_SMTP_HOST.getString();
		if (StringUtils.isBlank(smtpHostAddress)) {
			EmailUtils.log.warn(String.format(
				"No HOST address to send the e-mail, the intended recipients (%s) won't receive an e-mail.",
				recipients));
			return;
		}

		int smtpHostPort = Setting.MAIL_SMTP_PORT.getInt();
		if ((smtpHostPort < 1) || (smtpHostPort > 0xFFFF)) {
			EmailUtils.log.warn(String.format(
				"SMTP Port [%d] is out of range (1-65535), the intended recipients (%s) won't receive an e-mail",
				smtpHostPort, recipients));
			return;
		}

		if (!EmailUtils.validateSmtp(smtpHostAddress, smtpHostPort)) {
			EmailUtils.log.warn(String.format(
				"Host [%s] is not running an SMTP server on port 25. The intended recipients (%s) won't receive an e-mail.",
				smtpHostAddress, recipients));
			return;
		}

		if (EmailUtils.log.isTraceEnabled()) {
			EmailUtils.log.info(String.format("Sending this message as [%s] via [%s], to %s:%n%n%s%n", mailFromAddress,
				smtpHostAddress, recipients, message));
		} else {
			EmailUtils.log.info(String.format("Sending the message as [%s] via [%s], to %s", mailFromAddress,
				smtpHostAddress, recipients));
		}
		EmailUtils.postMail(smtpHostAddress, recipients, subject, message, mailFromAddress);
	}

	private static boolean validateSmtp(String address, int port) {
		Socket s = null;
		try {
			InetAddress addx = InetAddress.getByName(address);
			s = new Socket(addx, 25);
			OutputStream out = s.getOutputStream();
			PrintWriter pw = new PrintWriter(out);
			InputStream in = s.getInputStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			// Read up...
			while (r.readLine() != null) {
				;
			}
			// Send the hello...
			pw.printf("HELO%n");
			while (true) {
				String str = r.readLine();
				if (str == null) {
					break;
				}
				// Make sure the command elicited a "OK" response
				if (str.matches("^\\s*250(\\s.*)?$")) { return true; }
			}
		} catch (Throwable t) {
			if (EmailUtils.log.isTraceEnabled()) {
				EmailUtils.log.trace("Exception raised trying to determine if localhost has an SMTP server running", t);
			}
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		return false;
	}
}