/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebMon is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebMon.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.config;

import org.apache.commons.mail.Email;

/**
 * The encryption enum.
 * @author Martin Vysny
 */
public enum EncryptionEnum {

	/**
	 * No security.
	 */
	NONE {

		@Override
		public void activate(final Email mail) {
			mail.setSSL(false);
			mail.setTLS(false);
		}
	},
	/**
	 * TLS security.
	 */
	TLS {

		@Override
		public void activate(final Email mail) {
			mail.setSSL(false);
			mail.setTLS(true);
		}
	},
	/**
	 * SSL security.
	 */
	SSL {

		@Override
		public void activate(final Email mail) {
			mail.setSSL(true);
			mail.setTLS(false);
		}
	};

	/**
	 * Activates given security on given mail.
	 * @param mail activate security on this mail.
	 */
	public abstract void activate(final Email mail);
}
