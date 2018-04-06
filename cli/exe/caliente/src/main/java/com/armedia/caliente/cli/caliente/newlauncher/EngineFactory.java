package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Collection;
import java.util.Set;

import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.cfg.Configuration;

public interface EngineFactory

< //
	S, // Session
	W extends SessionWrapper<S>, // SessionWrapper
	V, // Value Container

	EC extends ExportContext<S, V, ECF>, //
	ECF extends ExportContextFactory<S, W, V, EC, EE>, //
	EDF extends ExportDelegateFactory<S, W, V, EC, EE>, //
	EE extends ExportEngine<S, W, V, EC, ECF, EDF>, //

	IC extends ImportContext<S, V, ICF>, //
	ICF extends ImportContextFactory<S, W, V, IC, IE, ?>, //
	IDF extends ImportDelegateFactory<S, W, V, IC, IE>, //
	IE extends ImportEngine<S, W, V, IC, ICF, IDF> //
> {

	public String getName();

	public Set<String> getAliases();

	public CmfCrypt getCrypt();

	public EE getExportEngine(Configuration cfg);

	public IE getImportEngine(Configuration cfg);

	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers();

}