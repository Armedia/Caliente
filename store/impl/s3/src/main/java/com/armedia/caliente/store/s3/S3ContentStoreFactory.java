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
package com.armedia.caliente.store.s3;

import java.nio.file.Paths;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfContentStoreFactory;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfStore;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;

public class S3ContentStoreFactory extends CmfContentStoreFactory<S3ContentStore> {

	protected static final String DEFAULT_REGION = Region.US_EAST_1.id();

	static enum CredentialType {
		//
		ANONYMOUS {
			@Override
			public AwsCredentialsProvider build(CfgTools cfg) {
				return AnonymousCredentialsProvider.create();
			}
		}, //
		STATIC {

			@Override
			public AwsCredentialsProvider build(CfgTools cfg) {
				String accessKey = cfg.getString(S3ContentStoreSetting.ACCESS_KEY);
				String secretKey = cfg.getString(S3ContentStoreSetting.SECRET_KEY);
				String sessionToken = cfg.getString(S3ContentStoreSetting.SESSION_TOKEN);
				if (StringUtils.isNotBlank(sessionToken)) {
					return StaticCredentialsProvider
						.create(AwsSessionCredentials.create(accessKey, secretKey, sessionToken));
				}
				return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
			}

		}, //
		PROFILE {
			@Override
			public AwsCredentialsProvider build(CfgTools cfg) {
				ProfileCredentialsProvider.Builder builder = ProfileCredentialsProvider.builder();
				final String location = cfg.getString(S3ContentStoreSetting.PROFILE_LOCATION);
				final String profile = cfg.getString(S3ContentStoreSetting.PROFILE_NAME);

				final ProfileFile profileFile;
				if (StringUtils.isBlank(location)) {
					profileFile = ProfileFile //
						.defaultProfileFile() //
					;
				} else {
					profileFile = ProfileFile //
						.builder() //
						.content(Tools.canonicalize(Paths.get(location))) //
						.build() //
					;
				}

				builder.profileFile(profileFile);

				if (StringUtils.isNotBlank(profile)) {
					builder.profileName(profile);
				}

				return builder.build();
			}
		}, //
		SYSTEM {
			@Override
			public AwsCredentialsProvider build(CfgTools cfg) {
				return EnvironmentVariableCredentialsProvider.create();
			}
		}, //
		ENVIRONMENT {
			@Override
			public AwsCredentialsProvider build(CfgTools cfg) {
				return SystemPropertyCredentialsProvider.create();
			}
		}, //
			//
		;

		public abstract AwsCredentialsProvider build(CfgTools cfg);

	}

	public S3ContentStoreFactory() {
		super("s3");
	}

	@Override
	protected S3ContentStore newInstance(CmfStore<?> parent, StoreConfiguration configuration, boolean cleanData,
		Supplier<CfgTools> prepInfo) throws CmfStorageException {
		// It's either direct, or taken from Spring or JNDI
		return new S3ContentStore(parent, new CfgTools(configuration.getEffectiveSettings()), cleanData);
	}
}