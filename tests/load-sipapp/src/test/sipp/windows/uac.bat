@call "setEnv.cmd"
@title SIPp-UAC

@IF "%1" == "" goto USAGE
@IF "%2" == "" goto USAGE

%SIPP_EXE% %SIPP_HOST%:%SIPP_UAS_PORT% -m %NB_MESSAGE% -r %LOAD_CALL_RATE% -rp %RATE_PERIOD%  -i %SIPP_HOST% -p %SIPP_UAC_PORT% %SIPP_OPTIONS% %TRACE_OPTIONS% -sf ..\uac.xml  -rsa %AS% %1 %2 %3 %4 %5 %6

@goto DONE


:USAGE
echo Usage:
echo  - proxy: %0 -s proxy
echo  - B2B UA: %0 -s b2b
echo  - UAS: %0 -s uas

:DONE