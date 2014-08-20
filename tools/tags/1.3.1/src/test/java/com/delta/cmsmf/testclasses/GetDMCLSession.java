package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfLoginInfo;

// TODO: Auto-generated Javadoc
/**
 * The Class GetDMCLSession.
 */
public class GetDMCLSession {

	/** The docbase name. */
	private String docbaseName;

	/** The docbase user name. */
	private String docbaseUserName;

	/** The docbase password. */
	private String docbasePassword;

	/**
	 * Instantiates a new gets the dmcl session.
	 * 
	 * @param docbaseName
	 *            the docbase name
	 * @param docbaseUserName
	 *            the docbase user name
	 * @param docbasePassword
	 *            the docbase password
	 */
	public GetDMCLSession(String docbaseName, String docbaseUserName, String docbasePassword) {
		super();
		this.docbaseName = docbaseName;
		this.docbaseUserName = docbaseUserName;
		this.docbasePassword = docbasePassword;
	}

	/**
	 * Gets the session.
	 * 
	 * @return the session
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	public IDfSession getSession() throws DfException {
		IDfClient dfClient = DfClient.getLocalClient();

		// Prepare login object
		IDfLoginInfo li = new DfLoginInfo();
		li.setUser(this.docbaseUserName);
		li.setPassword(this.docbasePassword);
		li.setDomain(null);

		// Get a documentum session using session manager
		IDfSessionManager sessionManager = dfClient.newSessionManager();
		sessionManager.setIdentity(this.docbaseName, li);
		return sessionManager.getSession(this.docbaseName);

	}

	/**
	 * Gets the session.
	 * 
	 * @param docbaseName
	 *            the docbase name
	 * @param docbaseUserName
	 *            the docbase user name
	 * @param docbasePassword
	 *            the docbase password
	 * @return the session
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	public IDfSession getSession(String docbaseName, String docbaseUserName, String docbasePassword) throws DfException {
		this.docbaseName = docbaseName;
		this.docbaseUserName = docbaseUserName;
		this.docbasePassword = docbasePassword;

		return getSession();

	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
