package com.armedia.cmf.engine.cmis;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.cmf.engine.Crypt;
import com.armedia.cmf.engine.CryptException;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public class CmisSessionFactory extends SessionFactory<Session> {

	private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+$");
	private static final String ATMOMPUB_URL_TEMPLATE = "%s/api/%s/public/cmis/versions/%s/atom";

	private final org.apache.chemistry.opencmis.client.api.SessionFactory factory = SessionFactoryImpl.newInstance();
	private final Map<String, String> parameters;

	public CmisSessionFactory(CfgTools settings) throws CryptException {
		super(settings);
		Map<String, String> parameters = new HashMap<String, String>();

		for (CmisSessionSetting s : CmisSessionSetting.values()) {
			if ((s.getSessionParameter() == null) || !settings.hasValue(s)) {
				continue;
			}
			String v = settings.getString(s);
			switch (s) {
				case PASSWORD:
					// Decrypt the password
					try {
						String encrypted = v;
						String decrypted = Crypt.decrypt(encrypted);
						if (this.log.isDebugEnabled()) {
							this.log.debug(String.format("Successfully decrypted the password from [%s]", encrypted));
						}
						v = decrypted;
					} catch (CryptException e) {
						String msg = "Failed to decrypt the password value, using it as literal";
						if (this.log.isTraceEnabled()) {
							this.log.warn(msg, e);
						} else if (this.log.isDebugEnabled()) {
							this.log.warn("Failed to decrypt the password value, using it as literal");
						}
					}
					break;
				case BASE_URL:
					if (v.endsWith("/")) {
						v = FileNameTools.removeTrailingSeparators(v, '/');
					}
					String ver = settings.getString(CmisSessionSetting.API_VERSION);
					if (!CmisSessionFactory.VERSION_PATTERN.matcher(ver).matches()) {
						String bad = ver;
						ver = CmisSessionSetting.API_VERSION.getDefaultValue().toString();
						this.log.warn(String.format(
							"Illegal version identifier [%s] - using the default value of [%s]", bad, ver));
					}
					String repoId = settings.getString(CmisSessionSetting.REPOSITORY_ID);
					v = String.format(CmisSessionFactory.ATMOMPUB_URL_TEMPLATE, v, repoId, ver);
					break;
				default:
					break;
			}
			if (!StringUtils.isBlank(v)) {
				parameters.put(s.getSessionParameter(), v);
			}
		}
		if (!parameters.containsKey(SessionParameter.REPOSITORY_ID)) {
			parameters.put(SessionParameter.REPOSITORY_ID,
				Tools.toString(CmisSessionSetting.REPOSITORY_ID.getDefaultValue()));
		}
		if (!parameters.containsKey(SessionParameter.BINDING_TYPE)) {
			parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
		}
		this.parameters = Tools.freezeMap(parameters);
	}

	@Override
	public PooledObject<Session> makeObject() throws Exception {
		return new DefaultPooledObject<Session>(
			this.factory.createSession(new HashMap<String, String>(this.parameters)));
	}

	@Override
	public void destroyObject(PooledObject<Session> p) throws Exception {
		p.getObject().getBinding().close();
	}

	@Override
	public boolean validateObject(PooledObject<Session> p) {
		return (p != null);
	}

	@Override
	public void activateObject(PooledObject<Session> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<Session> p) throws Exception {
	}

	@Override
	protected CmisSessionWrapper newWrapper(Session session) throws Exception {
		return new CmisSessionWrapper(this, session);
	}
}