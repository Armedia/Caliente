package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.PluggableServiceLocator.ErrorListener;
import com.armedia.commons.utilities.Tools;

public abstract class CalienteModule {

	protected static final Logger LOG = LoggerFactory.getLogger(CalienteModule.class);

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

	protected final Logger log = LoggerFactory.getLogger(getClass());

	public abstract Set<OperationMode> getSupportedModes();

	public abstract OptionSchemeExtensionSupport getOptionSchemeExtensionSupport(OperationMode mode);

	static Iterable<CalienteModule> discover() {
		return new Iterable<CalienteModule>() {
			private final PluggableServiceLocator<CalienteModule> locator = new PluggableServiceLocator<>(
				CalienteModule.class);

			// Constructor...
			{
				this.locator.setErrorListener(new ErrorListener() {
					@Override
					public void errorRaised(Class<?> serviceClass, Throwable t) {
						if (CalienteModule.LOG.isDebugEnabled()) {
							CalienteModule.LOG.error(
								"Failed to initialize an instance of the CalienteModule subclass [{}]",
								serviceClass.getCanonicalName(), t);
						} else {
							CalienteModule.LOG.error(
								"Failed to initialize an instance of the CalienteModule subclass [{}] ({}: {})",
								serviceClass.getCanonicalName(), t.getClass().getCanonicalName(), t.getMessage());
						}
					}
				});
				this.locator.setHideErrors(false);
			}

			@Override
			public Iterator<CalienteModule> iterator() {
				return this.locator.getAll();
			}
		};
	}
}