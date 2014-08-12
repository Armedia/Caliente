package com.delta.cmsmf.mainEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.documentum.fc.tools.RegistryPasswordUtils;

/**
 * The main method of this class is an entry point for the cmsmf application.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_encrypt {

	CMSMFMain_encrypt() throws Throwable {
		// prompt the user to enter the password that needs to be encrypted
		System.out.print("Enter the password that you would like to encrypt: ");

		// open up standard input
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String password = null;
		// read the password from the command-line; need to use try/catch with the
		// readLine() method
		try {
			password = br.readLine();
		} catch (IOException ioe) {
			System.out.println("IO error trying to read the password!");
			System.exit(1);
		}

		System.out.println("The encrypted password is: " + RegistryPasswordUtils.encrypt(password));
	}
}