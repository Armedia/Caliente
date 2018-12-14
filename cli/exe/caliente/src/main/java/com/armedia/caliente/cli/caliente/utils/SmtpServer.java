package com.armedia.caliente.cli.caliente.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import org.apache.commons.io.IOUtils;

import com.armedia.commons.utilities.Tools;

public class SmtpServer {

	public static enum SslMode {
		//
		NONE(25), //
		SSL(465), //
		TLS(587), //
		TLS_REQUIRED(587), //
		//
		;

		private final int defaultPort;

		private SslMode(int defaultPort) {
			this.defaultPort = defaultPort;
		}

		public static int getPort(SslMode mode) {
			if (mode == null) { return NONE.defaultPort; }
			return mode.defaultPort;
		}
	}

	/**
	 * More properties available at
	 * https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html
	 *
	 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
	 *
	 */
	private static enum Property {
		//
		mail_smtp_host, //
		mail_smtp_port, //
		mail_smtp_starttls_enable, //
		mail_smtp_starttls_required, //
		mail_smtp_ssl_enable, //
		mail_smtp_ssl_checkserveridentity, //
		mail_smtp_auth, //
		//
		;

		private final String id;

		private Property() {
			this.id = name().replace('_', '.');
		}
	}

	private final String address;
	private final InetAddress inetAddress;
	private final int port;
	private final SslMode sslMode;
	private final Authenticator authenticator;

	public SmtpServer(String address) throws Exception {
		this(address, SslMode.NONE);
	}

	public SmtpServer(String address, SslMode ssl) throws Exception {
		this(address, ssl, null);
	}

	public SmtpServer(String address, Authenticator authenticator) throws Exception {
		this(address, SslMode.NONE, authenticator);
	}

	public SmtpServer(String address, SslMode ssl, Authenticator authenticator) throws Exception {
		this(address, SslMode.getPort(ssl), ssl, authenticator);
	}

	public SmtpServer(String address, int port) throws Exception {
		this(address, port, SslMode.NONE);
	}

	public SmtpServer(String address, int port, SslMode ssl) throws Exception {
		this(address, port, ssl, null);
	}

	public SmtpServer(String address, int port, Authenticator authenticator) throws Exception {
		this(address, port, SslMode.NONE, authenticator);
	}

	public SmtpServer(String address, int port, SslMode ssl, Authenticator authenticator) throws Exception {
		this.inetAddress = SmtpServer.validateSmtp(address, port, Tools.coalesce(ssl, SslMode.NONE));
		if (this.inetAddress == null) { throw new Exception(
			String.format("Could not verify if an SMTP host was running at [%s] on port %d", address, port)); }
		this.port = port;
		this.address = address;
		this.authenticator = authenticator;
		this.sslMode = ssl;
	}

	public String getAddress() {
		return this.address;
	}

	public InetAddress getInetAddress() {
		return this.inetAddress;
	}

	public int getPort() {
		return this.port;
	}

	public SslMode getSslMode() {
		return this.sslMode;
	}

	public Authenticator getAuthenticator() {
		return this.authenticator;
	}

	public SmtpServer postMessage(SmtpMessage message) throws MessagingException {
		return postMessage(message, null);
	}

	public SmtpServer postMessage(SmtpMessage message, Map<String, ?> extraHeaders) throws MessagingException {
		boolean debug = false;

		// Set the host smtp inetAddress
		Properties props = new Properties();
		props.put(Property.mail_smtp_host.id, this.address);
		props.put(Property.mail_smtp_port.id, Tools.toString(this.port));

		Property sslProp = null;
		switch (this.sslMode) {
			case NONE:
				break;

			case SSL:
				sslProp = Property.mail_smtp_ssl_enable;
				break;

			case TLS_REQUIRED:
				props.put(Property.mail_smtp_starttls_required.id, "true");
				// fall-through

			case TLS:
				sslProp = Property.mail_smtp_starttls_enable;
				break;
		}

		if (sslProp != null) {
			props.put(sslProp.id, "true");
		}

		if (this.authenticator != null) {
			props.put(Property.mail_smtp_auth.id, Boolean.TRUE.toString());
		}

		// Get the default Session
		Session session = Session.getInstance(props, this.authenticator);
		session.setDebug(debug);

		// create a message
		Message msg = message.build(session);
		// Optional : You can also set your custom headers in the Email if you Want
		// msg.addHeader("X-CalienteHeader", "Caliente-Related");
		if (extraHeaders != null) {
			for (String name : extraHeaders.keySet()) {
				Object value = extraHeaders.get(name);
				if (value != null) {
					msg.addHeader(name, Tools.toString(value));
				}
			}
		}
		Transport.send(msg);
		return this;
	}

	private static InetAddress validateSmtp(String address, int port, SslMode sslMode) throws Exception {
		if ((port < 0) || (port > 65535)) { throw new Exception(String.format("Illegal port number given %d", port)); }
		Socket s = null;
		try {
			InetAddress addx = InetAddress.getByName(address);
			s = new Socket(addx, port);
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
				if (str.matches("^\\s*250(\\s.*)?$")) { return addx; }
			}
		} finally {
			IOUtils.closeQuietly(s);
		}
		return null;
	}
}