package com.delta.cmsmf.utils;

import java.io.File;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.constants.DctmTypeConstants;
import com.delta.cmsmf.mainEngine.CLIParam;
import com.delta.cmsmf.mainEngine.CMSMFLauncher;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;

public class CMSMFUtils {

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(CMSMFUtils.class);

	private static String cmsmfSyncCabinetName = Setting.STATE_CABINET_NAME.getString();
	private static String cmsmfLastExportObjName = Constant.LAST_EXPORT_OBJ_NAME;

	/**
	 * Gets the content path from content id.
	 *
	 * @param contentObjID
	 *            the content obj id
	 * @return the content path from content id
	 */
	public static String getContentPathFromContentID(String contentObjID) {
		String contentPath = "";
		String filePathSeparator = File.separator;
		if (contentObjID.length() == 16) {
			// 16 character object id in dctm consists of first 2 chars of obj type, next 6 chars of
// docbase
			// id in hex and last 8 chars server generated. We will use first 6 characters of this
// last 8
			// characters and generate the unique path.
			// For ex: if the id is 0600a92b80054db8 than the path would be 80\05\4d
			contentPath = contentObjID.substring(8, 16);
			contentPath = new StringBuffer(contentPath.substring(0, 2)).append(filePathSeparator)
				.append(contentPath.subSequence(2, 4)).append(filePathSeparator).append(contentPath.subSequence(4, 6))
				.toString();
		}

		return contentPath;

	}

	/**
	 * Runs a dctm job by given name.
	 *
	 * @param dctmSession
	 *            the dctm session
	 * @param jobName
	 *            the job name
	 * @throws DfException
	 *             the df exception
	 */
	public static void runDctmJob(IDfSession dctmSession, String jobName) throws DfException {
		// Set run_now attribute of a job to true to run a job.
		IDfSysObject oJob = (IDfSysObject) dctmSession.getObjectByQualification("dm_job where object_name = '"
			+ jobName + "'");
		oJob.setBoolean(DctmAttrNameConstants.RUN_NOW, true);
		oJob.save();
	}

	private static IDfSysObject getCmsmfStateObject(IDfSession dctmSession, boolean createIfMissing) throws DfException {
		final String targetDocbaseName = CMSMFLauncher.getParameter(CLIParam.docbase);
		final String cabinetPath = "/" + CMSMFUtils.cmsmfSyncCabinetName;
		final String folderPath = cabinetPath + "/" + targetDocbaseName;
		final String documentPath = folderPath + "/" + CMSMFUtils.cmsmfLastExportObjName;
		IDfSysObject lstExportObj = (IDfSysObject) dctmSession.getObjectByPath(documentPath);
		if ((lstExportObj == null) && createIfMissing) {
			// Object does not exist, create one.
			// try to locate a folder for a target repository and create one if it doesn't exist
			IDfFolder trgtDocbaseFolder = dctmSession.getFolderByPath(folderPath);
			if (trgtDocbaseFolder == null) {
				// target folder does not exist, create one.
				// try to locate the cmsmf_sync cabinet and create one if it doesn't exist
				IDfFolder cmsmfSyncCabinet = dctmSession.getFolderByPath(cabinetPath);
				if (cmsmfSyncCabinet == null) {
					CMSMFUtils.logger.info("Creating cabinet: " + CMSMFUtils.cmsmfSyncCabinetName
						+ " in source repository");
					// create the cabinet and folder underneath
					cmsmfSyncCabinet = (IDfFolder) dctmSession.newObject(DctmTypeConstants.DM_CABINET);
					cmsmfSyncCabinet.setObjectName(CMSMFUtils.cmsmfSyncCabinetName);
					cmsmfSyncCabinet.setHidden(true);
					cmsmfSyncCabinet.save();
				}

				// create a folder for a target repository in this cabinet.
				trgtDocbaseFolder = (IDfFolder) dctmSession.newObject(DctmTypeConstants.DM_FOLDER);
				trgtDocbaseFolder.setObjectName(targetDocbaseName);
				trgtDocbaseFolder.link(cmsmfSyncCabinet.getObjectId().getId());
				trgtDocbaseFolder.save();
			}
			// Create the object
			lstExportObj = (IDfSysObject) dctmSession.newObject(DctmTypeConstants.DM_DOCUMENT);
			lstExportObj.setObjectName(CMSMFUtils.cmsmfLastExportObjName);
			lstExportObj.link(trgtDocbaseFolder.getObjectId().getId());
			lstExportObj.save();
		}
		return lstExportObj;
	}

	/**
	 * Gets the last export date.
	 *
	 * @param dctmSession
	 *            the dctm session
	 * @return the last export date
	 */
	public static String getLastExportDate(IDfSession dctmSession) {
		String lastExportDate = "";
		try {
			// Try to locate the last export object to read the date from subject attribute
			IDfSysObject lstExportObj = CMSMFUtils.getCmsmfStateObject(dctmSession, false);
			final String message;
			if (lstExportObj != null) {
				lastExportDate = lstExportObj.getSubject();
				message = String.format("The last export date was [%s]", lastExportDate);
			} else {
				message = "No previous export date";
			}
			if (CMSMFUtils.logger.isInfoEnabled()) {
				CMSMFUtils.logger.info(message);
			}
			return lastExportDate;
		} catch (DfException e) {
			CMSMFUtils.logger.error("Error occured while retrieving last export run date", e);
		}
		return lastExportDate;
	}

	/**
	 * Sets the last export date.
	 *
	 * @param dctmSession
	 *            the dctm session
	 * @param exportDate
	 *            the export date
	 */
	public static void setLastExportDate(IDfSession dctmSession, String exportDate) {
		try {
			// Try to locate the last export object to read the date from subject attribute
			IDfSysObject lstExportObj = CMSMFUtils.getCmsmfStateObject(dctmSession, true);
			lstExportObj.setSubject(exportDate);
			lstExportObj.save();
			if (CMSMFUtils.logger.isInfoEnabled()) {
				CMSMFUtils.logger.info(String.format("Last export date set to [%s]", exportDate));
			}
		} catch (DfException e) {
			CMSMFUtils.logger.error("Error occured while setting last export run date", e);
		}
	}

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
	public static void postMail(String smtpHost, String recipients[], String subject, String message, String from)
		throws MessagingException {
		boolean debug = false;

		// Set the host smtp address
		Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);

		// Get the default Session
		Session session = Session.getDefaultInstance(props, null);
		session.setDebug(debug);

		// create a message
		Message msg = new MimeMessage(session);

		// set the from and to addresses
		InternetAddress addressFrom = new InternetAddress(from);
		msg.setFrom(addressFrom);

		InternetAddress[] addressTo = new InternetAddress[recipients.length];
		for (int i = 0; i < recipients.length; i++) {
			addressTo[i] = new InternetAddress(recipients[i]);
		}
		msg.setRecipients(Message.RecipientType.TO, addressTo);

		// Optional : You can also set your custom headers in the Email if you Want
		msg.addHeader("MyHeaderName", "CMSMF_INFO_HEADER");

		// Setting the Subject and Content Type
		msg.setSubject(subject);
		msg.setContent(message, "text/plain");
		Transport.send(msg);
	}

	/**
	 * Post cmsmf mail.
	 *
	 * @param subject
	 *            the subject of the email message
	 * @param message
	 *            the body of the email
	 * @throws MessagingException
	 *             the messaging exception
	 */
	public static void postCmsmfMail(String subject, String message) throws MessagingException {

		String mailRecipients = Setting.MAIL_RECIPIENTS.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(mailRecipients);
		String[] recipients = strTokenizer.getTokenArray();

		String mailFromAddress = Setting.MAIL_FROM_ADDX.getString();

		String smtpHostAddress = Setting.MAIL_SMTP_HOST.getString();

		if ((recipients.length == 0) || StringUtils.isBlank(mailFromAddress) || StringUtils.isBlank(smtpHostAddress)) {
			CMSMFUtils.logger.error("Please check recipients, mail from address or smtp host address"
				+ " properties in CMSMF_app.properties. Unable to post an email from CMSMF application.");
		} else {
			CMSMFUtils.postMail(smtpHostAddress, recipients, subject, message, mailFromAddress);
		}
	}
}
