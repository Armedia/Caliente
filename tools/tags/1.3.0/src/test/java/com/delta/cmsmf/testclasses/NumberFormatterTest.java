package com.delta.cmsmf.testclasses;

import java.text.NumberFormat;

public class NumberFormatterTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(0);
		System.out.println(nf.format(20000000));
	}
}
