package com.armedia.caliente.cli.caliente.options;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.armedia.caliente.cli.OptionValueFilter;

public class CLIFilters {

	static final OptionValueFilter EMAIL_FILTER = new OptionValueFilter() {

		@Override
		protected boolean checkValue(String value) {
			try {
				InternetAddress.parse(value);
				return true;
			} catch (AddressException e) {
				return false;
			}
		}

		@Override
		public String getDefinition() {
			return "E-mail address (as per RFC822)";
		}
	};

	static final OptionValueFilter INET_ADDX_FILTER = new OptionValueFilter() {

		@Override
		protected boolean checkValue(String value) {
			try {
				InetAddress.getByName(value);
				return true;
			} catch (UnknownHostException e) {
				return false;
			}
		}

		@Override
		public String getDefinition() {
			return "Dotted-IP-address or a valid hostname";
		}

	};

	/*
	static final OptionValueFilter URI_FILTER = new OptionValueFilter() {
	
		@Override
		protected boolean checkValue(String value) {
			try {
				new URI(value);
				return true;
			} catch (URISyntaxException e) {
			}
			return false;
		}
	
		@Override
		public String getDefinition() {
			return "A URI (as per java.net.URI)";
		}
	
	};
	
	static final OptionValueFilter URL_FILTER = new OptionValueFilter() {
	
		@Override
		protected boolean checkValue(String value) {
			try {
				new URL(value);
				return true;
			} catch (MalformedURLException e) {
			}
			return false;
		}
	
		@Override
		public String getDefinition() {
			return "A URL (as per java.net.URL)";
		}
	
	};
	*/
}