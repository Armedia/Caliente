param(
	[Parameter(Mandatory=$true)]
	[string]$MigrationRun,

	[Parameter(Mandatory=$false)]
	[string]$Root = "D:\2022-08-22_calienteMigrationToSharePoint",

	[Parameter(Mandatory=$false)]
	[string]$Caliente = "D:\caliente-4.0.4-beta-17-exe.jar",

	[Parameter(Mandatory=$true)]
	[string]$Payload,

	[Parameter(Mandatory=$false)]
	[string]$ExtractionServer = "https://bridgesdm.dot.nycnet/alfresco/api/-default-/public/cmis/versions/1.1/browser",

	[Parameter(Mandatory=$false)]
	[string]$ExtractionUser = "admin",

	[Parameter(Mandatory=$true)]
	[string]$ExtractionPassword
)

## Extract
try
{
	if (-NOT (Test-Path -Path ${Root}))
	{
		mkdir ${Root} > $null
	}
	Push-Location ${Root}
	& java -jar ${Caliente} `
		--engine cmis export `
		--server ${ExtractionServer} `
		--user ${ExtractionUser} `
		--password ${ExtractionPassword} `
		--from ${Payload} `
		--data ${MigrationRun} `
		--only-types DOCUMENT,FOLDER
} finally {
	Pop-Location
}