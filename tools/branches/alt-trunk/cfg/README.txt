Execute run_cmsmf.bat file to run the application. You may have to change few of the properties in beginning of this batch file to point it to proper dctm instllation directory as well as shared and config directory.

There is a file called CMSMF_app.properties in config folder. You will need to set several properties in this file for the execution. There are properties for Import/Export mode of operation, Export Query Predicate and repository connection parameters.

Lastly, there is a CMSMF_log4j.properties file in config folder. Copy the content of this file at the end of the log4j.properties file found in DOCUMENTUM_CONFIG folder. By doing this, we will have separate log files for cmsmf application. You may have to change log file paths for cmsmf appenders to match the dctm logfile locations.
