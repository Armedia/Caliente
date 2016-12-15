package com.armedia.caliente.tools;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.launcher.LaunchParameterSet;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.Parameter;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfTime;

/**
 * This class is used as a testbed to run quick'n'dirty DFC test programs
 *
 * @author diego.rivera@armedia.com
 *
 */
public class Scratchpad extends AbstractLauncher implements LaunchParameterSet {

	public static final void main(String... args) {
		System.exit(new Scratchpad().launch(args));
	}

	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

	@Override
	public Collection<? extends Parameter> getParameters(CommandLineValues commandLine) {
		return Collections.emptyList();
	}

	@Override
	protected Collection<? extends LaunchParameterSet> getLaunchParameterSets(CommandLineValues cli, int pass) {
		return null;
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(CommandLineValues cli) {
		return Arrays.asList(this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName(int pass) {
		return "Caliente Scratchpad";
	}

	@Override
	protected int run(CommandLineValues cli) throws Exception {
		final DfcSessionPool pool = new DfcSessionPool("documentum", "dmadmin2", "ArM3D!A");
		final IDfSession session = pool.acquireSession();
		try {
			final IDfLocalTransaction tx = DfUtils.openTransaction(session);
			boolean ok = false;
			try {
				IDfFolder parent = session.getFolderByPath("/CMSMFTests/Specials");

				IDfSysObject obj = IDfSysObject.class.cast(session.newObject("sysobject_child_test"));
				obj.setObjectName("SysObject Child Test.bin");
				obj.setContentType("binary");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(obj.getObjectName().getBytes());
				obj.setContent(baos);
				for (int i = 0; i < 10; i++) {
					obj.setRepeatingString("property_one", i, UUID.randomUUID().toString());
				}
				obj.setInt("property_two", 10);
				obj.link(parent.getObjectId().getId());
				obj.save();

				obj = IDfSysObject.class.cast(session.newObject("sysobject_grandchild_test"));
				obj.setObjectName("SysObject Grandchild Test.bin");
				obj.setContentType("binary");
				baos = new ByteArrayOutputStream();
				baos.write(obj.getObjectName().getBytes());
				obj.setContent(baos);
				for (int i = 0; i < 10; i++) {
					obj.setRepeatingString("property_one", i, UUID.randomUUID().toString());
				}
				obj.setInt("property_two", 10);
				obj.setTime("property_three", new DfTime(new Date()));
				obj.link(parent.getObjectId().getId());
				obj.save();

				DfUtils.commitTransaction(session, tx);
				ok = true;
			} finally {
				if (!ok) {
					DfUtils.abortTransaction(session, tx);
				}
			}
		} finally {
			pool.releaseSession(session);
		}
		return 0;
	}
}