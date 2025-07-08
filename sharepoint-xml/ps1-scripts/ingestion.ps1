param(
	[Parameter(Mandatory=$true)]
	[string]$MigrationRun,

	[Parameter(Mandatory=$false)]
	[string]$Root = "D:\2022-08-22_calienteMigrationToSharePoint",

	[Parameter(Mandatory=$false)]
	[string]$Ingestor = "C:\Armedia LLC\Caliente SharePoint Importer\Caliente.SharePoint.Import.exe",

	[Parameter(Mandatory=$false)]
	[string]$SharePointURL = "https://nycdot.sharepoint.com/sites/Bridges_QA",

	[Parameter(Mandatory=$false)]
	[string]$SharePointUser = "AlfMigrator@dot.nyc.gov"

	[Parameter(Mandatory=$true)]
	[string]$SharePointPassword
)

## Ingest
try
{
	Push-Location ${Root}
@"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE import>
<import>
	<library>Documents</library>
	<fallbackUser>drivera@armedia.com</fallbackUser>
	<internalUser>drivera@armedia.com</internalUser>
	<fallbackGroup>AntwerpDemo</fallbackGroup>
	<internalGroup>AntwerpDemo</internalGroup>
	<fallbackDocumentType>Document</fallbackDocumentType>
	<fallbackFolderType>Folder</fallbackFolderType>
	<threads>5</threads>
	<reuseCount>10</reuseCount>
	<cleanTypes>true</cleanTypes>
	<simulationMode>none</simulationMode>
	<locationMode>current</locationMode>
	<fixExtensions>true</fixExtensions>
	<autoPublish>true</autoPublish>
	<retries>10</retries>
	<cmsmf.indexOnly>false</cmsmf.indexOnly>
</import>
"@ | Out-File -Encoding UTF8 "config.xml"

	& "${Ingestor}" --cfg "config.xml" --data ${MigrationRun} --siteUrl ${SharePointURL} --user ${SharePointUser} --password ${SharePointPassword}
} finally {
	Pop-Location
}