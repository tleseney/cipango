@call "setEnv.cmd"
title SIPp-UAC MESSAGE

%SIPP_EXE% %AS_HOST%:%AS_PORT% -m %NB_MESSAGE% -r %LOAD_CALL_RATE% -rp %RATE_PERIOD%  -i %SIPP_HOST% -p %SIPP_UAC_PORT% %SIPP_OPTIONS% %TRACE_OPTIONS% -sf ..\message.xml -s uas
