package com.delta.cmsmf.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.documentum.fc.common.DfException;
import com.documentum.fc.tools.RegistryPasswordUtils;

/**
 * The Class EncryptPasswordUtil contains static methods to encrypt/decrypt password strings using
 * DFC.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class EncryptPasswordUtil {

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws DfException
	 *             the df exception
	 */
	public static void main(String[] args) throws DfException {

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

	/**
	 * Decrypt password.
	 * 
	 * @param encryptedPassword
	 *            the encrypted password
	 * @return the string
	 * @throws DfException
	 *             the df exception
	 */
	public static String decryptPassword(String encryptedPassword) throws DfException {

		return RegistryPasswordUtils.decrypt(encryptedPassword);
	}

	/**
	 * Encrypt password.
	 * 
	 * @param password
	 *            the password
	 * @return the string
	 * @throws DfException
	 *             the df exception
	 */
	public static String encryptPassword(String password) throws DfException {
		return RegistryPasswordUtils.encrypt(password);
	}

}
