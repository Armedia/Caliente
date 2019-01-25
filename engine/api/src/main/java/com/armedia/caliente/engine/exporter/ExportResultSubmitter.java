package com.armedia.caliente.engine.exporter;

@FunctionalInterface
public interface ExportResultSubmitter {
	public void submit(ExportTarget target);
}