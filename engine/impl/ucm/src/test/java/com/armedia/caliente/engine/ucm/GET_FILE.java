package com.armedia.caliente.engine.ucm;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import oracle.stellent.ridc.IdcClient;
import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.model.TransferFile;
import oracle.stellent.ridc.protocol.ServiceResponse;

@SuppressWarnings({
	"rawtypes", "unused"
})
public class GET_FILE {

	private final static String idcConnectionURL = "idc://localhost:4444";
	private final static String username = "sysadmin";

	public IdcClient getUCMConnection() throws IdcClientException, IOException {

		IdcClientManager clientManager = new IdcClientManager();
		IdcClient client = clientManager.createClient(GET_FILE.idcConnectionURL);
		IdcContext userContext = new IdcContext(GET_FILE.username);
		return client;

	}

	public void getFile(String dID) throws IdcClientException, IOException {
		// Get the client (from the base class) and create a new binder
		System.out.println("In The getFile method.. ");
		System.out.println("This is the dID.. " + dID);
		IdcClient client = getUCMConnection();
		DataBinder dataBinder = client.createBinder();
		dataBinder.putLocal("IdcService", "GET_FILE");
		dataBinder.putLocal("dID", dID);
		IdcContext userContext = new IdcContext(GET_FILE.username);
		ServiceResponse response = client.sendRequest(userContext, dataBinder);
		int reportedSize = Integer.parseInt(response.getHeader("Content-Length"));
		int retrievedSize = 0;
		String contentsInHex = "";
		// The file is streamed back to us in the response
		InputStream fstream = response.getResponseStream();

		try {
			// FileInputStream fstream = new response.getResponseStream ();
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String str;
			while ((str = br.readLine()) != null) {
				System.out.println(str);
			}
			in.close();
		} catch (Exception e) {
			System.err.println(e);
		}

		response.close();
		System.out.println("This is in the After conversion");
	}

	public void createUser() throws IdcClientException, IOException {

		System.out.println("In The createUser method.. ");
		IdcClient client = getUCMConnection();
		DataBinder dataBinder = client.createBinder();
		dataBinder.putLocal("IdcService", "ADD_USER");
		dataBinder.putLocal("dName", "TEST_ROLE");
		dataBinder.putLocal("dFullName", "TEST_ROLE");
		dataBinder.putLocal("dPassword", "ibm123$$");
		dataBinder.putLocal("dUserAuthType", "Local");
		dataBinder.putLocal("userIsAdmin", "true");
		dataBinder.putLocal("dEmail", "TEST_ROLE@gmail.com");
		IdcContext userContext = new IdcContext(GET_FILE.username);
		client.sendRequest(userContext, dataBinder);
		System.out.println("After Creating the USER.. ");

	}

	public void getUserList() throws IdcClientException, IOException {

		System.out.println("In The getUserList method.. ");
		IdcClient client = getUCMConnection();
		DataBinder dataBinder = client.createBinder();
		dataBinder.putLocal("IdcService", "GET_USERS");
		IdcContext userContext = new IdcContext(GET_FILE.username);
		ServiceResponse response = client.sendRequest(userContext, dataBinder);
		DataBinder resultSet = response.getResponseAsBinder();

		System.out.println("Resultset Names :" + resultSet.getResultSetNames());
		// Collection coll = responseData.getResultSetNames();
		DataResultSet drs = resultSet.getResultSet("Users");
		DataResultSet usrattri = resultSet.getResultSet("UserAttribInfo");

		for (DataObject dataObject : drs.getRows()) {

			System.out.println("User Name :" + dataObject.get("dFullName"));
		}
		for (DataObject dataObject : usrattri.getRows()) {

			System.out.println(" User Name :" + dataObject.get("dUserName"));
			System.out.println(" Role :" + dataObject.get("AttributeInfo"));
		}

		response.close();
		System.out.println("After the getUserList method.. ");

	}

	public void addGroup() throws IdcClientException, IOException {

		System.out.println("In The addRole method.. ");
		IdcClient client = getUCMConnection();
		DataBinder dataBinder = client.createBinder();
		dataBinder.putLocal("IdcService", "ADD_GROUP");
		dataBinder.putLocal("dGroupName", "TEST_GROUP");
		dataBinder.putLocal("dPrivilege", "15");
		dataBinder.putLocal("dDescription", "admin privileges");
		IdcContext userContext = new IdcContext(GET_FILE.username);
		client.sendRequest(userContext, dataBinder);

	}

	public void checkinFile(String Filename, String Filepath) throws IdcClientException, IOException {

		System.out.println("In The createUser method.. ");
		IdcClient client = getUCMConnection();
		DataBinder binder = client.createBinder();

		System.out.println("In the checkinFile method.. ");
		binder.putLocal("IdcService", "CHECKIN_UNIVERSAL");
		binder.putLocal("dDocTitle", "Test_Ridc");
		binder.putLocal("dDocName", "Test_Ridc");
		binder.putLocal("dDocType", "Document");
		binder.putLocal("dDocAuthor", "sysadmin");
		binder.putLocal("dSecurityGroup", "Public");
		binder.putLocal("dDocAccount", "");
		binder.putLocal("dOriginalName", Filename);
		binder.addFile("primaryFile", new TransferFile(new File(Filepath + Filename)));
		IdcContext userContext = new IdcContext(GET_FILE.username);
		client.sendRequest(userContext, binder);
		System.out.println("After checkinFile method.. ");
	}

	public void checkOutByName(String DocName, String DocTitle) throws IdcClientException, IOException {

		System.out.println("In The createUser method.. ");
		IdcClient client = getUCMConnection();
		DataBinder binder = client.createBinder();

		System.out.println("In the checkOutByName method.. ");
		binder.putLocal("IdcService", "CHECKOUT_BY_NAME");
		binder.putLocal("dDocName", "DHANU_000048");
		binder.putLocal("dDocTitle", "TEST...");
		IdcContext userContext = new IdcContext(GET_FILE.username);
		client.sendRequest(userContext, binder);
		System.out.println("After checkOutByName method.. ");

	}

	public void deleteDoc() throws IdcClientException, IOException {

		System.out.println("In the deleteDoc method.. ");
		IdcClient client = getUCMConnection();
		DataBinder binder = client.createBinder();
		binder.putLocal("IdcService", "DELETE_DOC");
		binder.putLocal("dID", "57");
		binder.putLocal("dDocName", "TESTUPLOADING..");
		IdcContext userContext = new IdcContext(GET_FILE.username);
		client.sendRequest(userContext, binder);
		System.out.println("After deleteDoc method.. ");

	}

	public void updateDoc() throws IdcClientException, IOException {

		System.out.println("In The createUser method.. ");
		IdcClient client = getUCMConnection();
		DataBinder binder = client.createBinder();
		System.out.println("In the updateDoc method.. ");
		binder.putLocal("IdcService", "UPDATE_DOCINFO");
		binder.putLocal("dDocName", "DHANU_000048");
		binder.putLocal("dID", "54");
		binder.putLocal("dDocAuthor", "weblogic");
		binder.putLocal("dDocTitle", "TEST00");
		binder.putLocal("xlanguage", "UPDATE_TELUGU..");
		IdcContext userContext = new IdcContext(GET_FILE.username);
		client.sendRequest(userContext, binder);
		System.out.println("After updateDoc method.. ");
	}

	public void getSearchResults(String querytext) throws IdcClientException, IOException {

		System.out.println("In The getSearchResults method.. ");
		IdcClient client = getUCMConnection();
		DataBinder dataBinder = client.createBinder();
		dataBinder.putLocal("IdcService", "GET_SEARCH_RESULTS");
		dataBinder.putLocal("QueryText", querytext);
		IdcContext userContext = new IdcContext(GET_FILE.username);
		ServiceResponse response = client.sendRequest(userContext, dataBinder);
		DataBinder binder = response.getResponseAsBinder();
		DataResultSet resultSet = binder.getResultSet("SearchResults");
		// loop over the results
		for (DataObject dataObject : resultSet.getRows()) {
			System.out.println(" Title : " + dataObject.get("dDocTitle") + " Author : " + dataObject.get("dDocAuthor")
				+ "    Security Group : " + dataObject.get("dSecurityGroup"));

		}

	}

	public static void main(String[] args) throws IdcClientException, FileNotFoundException, IOException, Exception {

		GET_FILE GF = new GET_FILE();
		GF.getFile("62");
		GF.createUser();
		GF.getUserList();
		GF.addGroup();
		GF.checkOutByName("FEB_2012_PAYSLIP", "Payslip_Feb_2012");
		GF.updateDoc();
		GF.getSearchResults("dDocAuthor <matches> `sysadmin` <AND> dDocType <matches> `Document`");
		GF.checkinFile("Payslip_Feb_2012.pdf", "C:/Documents and Settings/Administrator/Desktop/checkin_Docs/");
		GF.deleteDoc();

	}

}