package com.armedia.caliente.cli.caliente.launcher;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.PluggableServiceLocator.ErrorListener;
import com.armedia.commons.utilities.Tools;

public abstract class CalienteLauncherModule {

	private static final Logger LOG = Logger.getLogger(CalienteLauncherModule.class);

	public static final class OperationMode implements Comparable<OperationMode> {
		private final String cms;
		private final String mode;

		public OperationMode(String cms, String mode) {
			this.cms = cms;
			this.mode = mode;
		}

		public String getCms() {
			return this.cms;
		}

		public String getMode() {
			return this.mode;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.mode, this.cms);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			OperationMode other = OperationMode.class.cast(obj);
			if (!Tools.equals(this.mode, other.mode)) { return false; }
			if (!Tools.equals(this.cms, other.cms)) { return false; }
			return true;
		}

		@Override
		public int compareTo(OperationMode o) {
			if (o == null) { return 1; }
			int r = Tools.compare(this.mode, o.mode);
			if (r != 0) { return r; }
			return Tools.compare(this.cms, o.cms);
		}
	}

	public abstract Set<OperationMode> getSupportedModes();

	public abstract OptionSchemeExtensionSupport getOptionSchemeExtendensionSupport(OperationMode mode);

	static Iterable<CalienteLauncherModule> discover() {
		return new Iterable<CalienteLauncherModule>() {
			private final PluggableServiceLocator<CalienteLauncherModule> locator = new PluggableServiceLocator<>(
				CalienteLauncherModule.class);

			{
				this.locator.setErrorListener(new ErrorListener() {
					@Override
					public void errorRaised(Class<?> serviceClass, Throwable t) {
						if (CalienteLauncherModule.LOG.isDebugEnabled()) {
							CalienteLauncherModule.LOG.error(String.format(
								"Failed to initialize an instance of the CalienteLauncherModule subclass [%s]",
								serviceClass.getCanonicalName()), t);
						}
					}
				});
				this.locator.setHideErrors(false);
			}

			@Override
			public Iterator<CalienteLauncherModule> iterator() {
				return this.locator.getAll();
			}
		};
	}
}