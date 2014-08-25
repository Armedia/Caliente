@echo off

:: set documentum installation location
set dctm_install_dir=C:\Program Files\Documentum
:: set documentum shared location
set dctm_shared_dir=%dctm_install_dir%\Shared
:: set documentum config location
set dctm_config_dir=C:\Documentum\config

:: set classpath
set CMSMF_CLASSPATH="%dctm_install_dir%\dctm.jar"
set CMSMF_CLASSPATH=%CMSMF_CLASSPATH%;".\cmsmf.jar"
set CMSMF_CLASSPATH=%CMSMF_CLASSPATH%;".\lib\*"
set CMSMF_CLASSPATH=%CMSMF_CLASSPATH%;"%dctm_shared_dir%\*"
set CMSMF_CLASSPATH=%CMSMF_CLASSPATH%;"%dctm_config_dir%"
set CMSMF_CLASSPATH=%CMSMF_CLASSPATH%;".\config\CMSMF_app.properties"

:: set CLASSPATH=%CMSMF_CLASSPATH%
echo Classpath is set to: %CMSMF_CLASSPATH%

:: set various JVM options
set CMSMF_JVM_OPTIONS=-Xms64m -Xmx192m -Xss64k -Dcom.sun.management.jmxremote

:: java -verbose -classpath %CMSMF_CLASSPATH% -agentlib:jdwp=transport=dt_socket,server=y,address=8000 com.delta.cmsmf.mainEngine.RepoSyncMain > output.txt

java %CMSMF_JVM_OPTIONS% -classpath %CMSMF_CLASSPATH% com.delta.cmsmf.mainEngine.CMSMFMain

