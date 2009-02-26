/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebVM.
 *
 * WebVM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebVM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebVM.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.config;

import java.io.Serializable;

/**
 * The configuration bean.
 * @author Martin Vysny
 */
public final class Config implements Serializable {

	/**
	 * Creates new config file with default settings.
	 */
	public Config() {
		super();
	}

	/**
	 * Clones given config.
	 */
	public Config(final Config other) {
		super();
		Binder.copy(other, this);
	}
	/**
	 * Triggers a problem when there is less than minFreeDiskSpaceMb of free space on some drive
	 */
	@Bind(key = "minFreeDiskSpaceMb", min = 0)
	public int minFreeDiskSpaceMb = 100;
	/**
	 * Triggers a problem when GC uses over gcCpuTreshold% or more of CPU continuously for gcCpuTresholdSamples seconds.
	 */
	@Bind(key = "gcCpuTreshold", min = 0)
	public int gcCpuTreshold = 50;
	/**
	 * Triggers a problem when GC uses over gcCpuTreshold% or more of CPU continuously for gcCpuTresholdSamples seconds.
	 */
	@Bind(key = "gcCpuTresholdSamples", min = 1)
	public int gcCpuTresholdSamples = 3;
	/**
	 * If the memory usage after GC goes above this value the {@link #CLASS_GC_MEMORY_CLEANUP} problem is reported.
	 */
	@Bind(key = "memAfterGcUsageTreshold", min = 0, max = 100)
	public int memAfterGcUsageTreshold = 85;
	/**
	 * If the memory usage goes above this value the pool name is reported in the {@link #CLASS_MEMORY_STATUS} report. This never triggers a problem.
	 */
	@Bind(key = "memUsageTreshold", min = 0, max = 100)
	public int memUsageTreshold = 90;
	/**
	 * The SMTP server host/port. If this is commented then no mails are sent.
	 */
	@Bind(key = "mail.smtp.host")
	public String mailSmtpHost;
	/**
	 * SMTP server port, defaults to 25 (465 for SSL)
	 */
	@Bind(key = "mail.smtp.port", min = -1, max = 65535)
	public int mailSmtpPort = -1;
	/**
	 * The "from" address
	 */
	@Bind(key = "mail.from")
	public String mailFrom;
	/**
	 * Receivers, split by a comma
	 */
	@Bind(key = "mail.to")
	public String mailTo;
	/**
	 * The connection encryption.
	 */
	@Bind(key = "mail.smtp.encryption")
	public EncryptionEnum mailSmtpEncryption = EncryptionEnum.NONE;
	/**
	 * Optional SMTP authentication.
	 */
	@Bind(key = "mail.smtp.username")
	public String mailSmtpUsername;
	/**
	 * Optional SMTP authentification.
	 */
	@Bind(key = "mail.smtp.password")
	public String mailSmtpPassword;
}
