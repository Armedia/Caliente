package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;

public class UpdateUserTest {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {
		// TODO Auto-generated method stub

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		IDfUser dfUser = dctmSession.getUserByLoginName("D9105482", null);

		/*
		 * <user_address, Rohini.Gupta@delta.com> <user_group_name, pci_contrib> <owner_def_permit, 7>
		 * <user_privileges, 0> <user_login_domain, > <group_def_permit, 6> <user_global_unique_id, :D9105482>
		 * <acl_domain, dm_dbo> <user_login_name, D9105482> <world_def_permit, 1> <acl_name, PCI - Private>
		 * <user_os_name, D9105482> <failed_auth_attempt, 0> <globally_managed, false> <user_state, 1>
		 * <user_source, unix only> <client_capability, 2> <user_name, Gupta, Rohini> <workflow_disabled,
		 * true> <user_xprivileges, 0> <default_folder, /Temp>
		 */
		dfUser.setString("user_address", "Rohini.Gupta@delta.com");
		dfUser.setString("user_group_name", "pci_contrib");
		dfUser.setString("user_login_domain", "");
		dfUser.setString("user_global_unique_id", ":D9105482");
		dfUser.setString("acl_domain", "dm_dbo");
		dfUser.setString("user_login_name", "D9105482");
		dfUser.setString("acl_name", "PCI - Private");
		dfUser.setString("user_os_name", "D9105482");
		dfUser.setString("user_source", "unix only");
// dfUser.setString("user_name", "Gupta, Rohini");
		dfUser.setString("default_folder", "/Temp");

		dfUser.save();

		System.out.println("DONE!");

	}
}
