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
package com.armedia.caliente.cli.caliente.options;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.armedia.commons.utilities.cli.OptionValueFilter;

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

	static final OptionValueFilter MAIL_AUTH_FILTER = new OptionValueFilter() {

		@Override
		protected boolean checkValue(String value) {
			return false;
		}

		@Override
		public String getDefinition() {
			return "An SMTP authentication mode";
		}

	};
}