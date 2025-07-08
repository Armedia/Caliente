param(
	[Parameter(Mandatory=$false)]
	[string]$Root = "D:\2022-08-22_calienteMigrationToSharePoint",

	[Parameter(Mandatory=$true)]
	[string]$MigrationRun,

	[Parameter(Mandatory=$false)]
	[string]$Caliente = "D:\caliente-4.0.4-beta-17-exe.jar",

	[Parameter(Mandatory=$true)]
	[string]$Payload,

	[Parameter(Mandatory=$false)]
	[string]$ExtractionServer = "https://bridgesdm.dot.nycnet/alfresco/api/-default-/public/cmis/versions/1.1/browser",

	[Parameter(Mandatory=$false)]
	[string]$ExtractionUser = "admin",

	[Parameter(Mandatory=$true)]
	[string]$ExtractionPassword,

	[Parameter(Mandatory=$false)]
	[string]$Ingestor = "C:\Armedia LLC\Caliente SharePoint Importer\Caliente.SharePoint.Import.exe",

	[Parameter(Mandatory=$false)]
	[string]$SharePointURL = "https://nycdot.sharepoint.com/sites/Bridges_QA",

	[Parameter(Mandatory=$false)]
	[string]$SharePointUser = "AlfMigrator@dot.nyc.gov",

	[Parameter(Mandatory=$true)]
	[string]$SharePointPassword
)

try
{
	Push-Location (Split-Path $MyInvocation.MyCommand.Path)
	& .\extraction.ps1 -Root ${Root} -Caliente ${Caliente} -MigrationRun ${MigrationRun} -Payload ${Payload} -ExtractionServer ${ExtractionServer} -ExtractionUser ${ExtractionUser} -ExtractionPassword ${ExtractionPassword}
	if (!${?}) {
		"Extraction failed"
		exit 1
	}

	& .\transformation.ps1 -Root ${Root} -Caliente ${Caliente} -MigrationRun ${MigrationRun}
	if (!${?}) {
		"Transformation failed"
		exit 1
	}

	& .\ingestion.ps1 -Root ${Root} -Ingestor ${Ingestor} -MigrationRun ${MigrationRun} -SharePointURL ${SharePointURL} -SharePointUser ${SharePointUser} -SharePointPassword ${SharePointPassword}
	if (!${?}) {
		"Ingestion Failed"
		exit 1
	}
} finally {
	Pop-Location
}