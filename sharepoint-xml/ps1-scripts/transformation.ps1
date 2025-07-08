param(
	[Parameter(Mandatory=$true)]
	[string]$MigrationRun,

	[Parameter(Mandatory=$false)]
	[string]$Root = "D:\2022-08-22_calienteMigrationToSharePoint",

	[Parameter(Mandatory=$false)]
	[string]$Caliente = "D:\caliente-4.0.4-beta-17-exe.jar"
)

## Transform
try
{
	Push-Location ${Root}
	& java -jar ${Caliente} `
		--engine xml import `
		--data ${MigrationRun} `
		--trim-path 2
} finally {
	Pop-Location
}