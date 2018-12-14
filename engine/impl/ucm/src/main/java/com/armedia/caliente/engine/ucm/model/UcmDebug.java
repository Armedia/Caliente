package com.armedia.caliente.engine.ucm.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfValue;

import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;

public class UcmDebug {

	private static final String NULL = "<null>";

	public static String dumpBinder(String label, DataBinder binder) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		try {
			UcmDebug.dumpBinder(label, binder, pw);
		} finally {
			pw.flush();
			sw.flush();
		}
		return sw.toString();
	}

	public static void dumpBinder(String label, DataBinder binder, PrintWriter pw) {
		pw.printf("Binder %s%n", label);
		pw.printf("%s%n", StringUtils.repeat('-', 80));
		pw.printf("Local Data%n");
		pw.printf("%s%n", StringUtils.repeat('-', 60));
		UcmDebug.dumpDataObject(1, binder.getLocalData(), pw);
		pw.printf("Result Sets%n");
		pw.printf("%s%n", StringUtils.repeat('-', 60));
		for (String rs : binder.getResultSetNames()) {
			UcmDebug.dumpResultSet(rs, binder.getResultSet(rs), pw);
		}
	}

	public static String dumpDataObject(int indent, DataObject o) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		try {
			UcmDebug.dumpDataObject(indent, o, pw);
		} finally {
			pw.flush();
			sw.flush();
		}
		return sw.toString();
	}

	public static void dumpDataObject(int indent, DataObject o, PrintWriter pw) {
		final String indentStr = StringUtils.repeat('\t', indent);
		for (String s : new TreeSet<>(o.keySet())) {
			Object v = o.get(s);
			if (v == null) {
				v = UcmDebug.NULL;
			} else {
				v = String.format("[%s]", v);
			}
			pw.printf("%s[%s] -> %s%n", indentStr, s, v);
		}
	}

	public static String dumpAttributes(int indent, UcmAttributes o) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		try {
			UcmDebug.dumpObject(indent, o, pw);
		} finally {
			pw.flush();
			sw.flush();
		}
		return sw.toString();
	}

	public static void dumpObject(int indent, UcmAttributes o, PrintWriter pw) {
		final String indentStr = StringUtils.repeat('\t', indent);
		Map<String, CmfValue> m = o.getData();
		for (String s : new TreeSet<>(m.keySet())) {
			CmfValue v = m.get(s);
			String V = null;
			if (v == null) {
				V = UcmDebug.NULL;
			} else {
				V = String.format("[%s]", v.asString());
			}
			pw.printf("%s[%s] -> %s%n", indentStr, s, V);
		}
	}

	public static String dumpResultSet(String label, DataResultSet resultSet) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		try {
			UcmDebug.dumpResultSet(label, resultSet, pw);
		} finally {
			pw.flush();
			sw.flush();
		}
		return sw.toString();
	}

	public static void dumpResultSet(String label, DataResultSet resultSet, PrintWriter pw) {
		if (resultSet == null) {
			pw.printf("WARNING: ResultSet [%s] is null", label);
			return;
		}
		pw.printf("\tMap contents: %s%n", label);
		pw.printf("\t%s%n", StringUtils.repeat('-', 50));
		int i = 0;
		for (DataObject o : resultSet.getRows()) {
			pw.printf("\t\tItem [%d]:%n", i++);
			pw.printf("\t\t%s%n", StringUtils.repeat('-', 40));
			UcmDebug.dumpDataObject(3, o, pw);
		}
		pw.printf("%n");
		pw.flush();
		return;
	}
}