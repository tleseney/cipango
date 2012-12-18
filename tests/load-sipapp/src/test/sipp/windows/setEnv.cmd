@rem *************************************************************************
@rem @set SIPP env for load tests
@rem *************************************************************************

@rem TO ADAPT
@set SIPP_EXE="C:\Program Files\sipp\sipp"

@rem The limit for the load message
@set NB_MESSAGE= 100
@set AS=192.168.2.10


@rem The tester UAS port (default 5062)
@set SIPP_UAS_PORT=5062

@rem The tester UAC port (default 5064)
@set SIPP_UAC_PORT=5065

@rem The IP address of the tester host 
@set SIPP_HOST=192.168.2.10

@set SIPP_UAC_HOST=%SIPP_HOST%
@set SIPP_UAS_HOST=%SIPP_HOST%

@rem SIPP options like -nr (to disable retransmission in UDP mode) 
@rem or -nd (for no default)
@set SIPP_OPTIONS=-t u1

@rem default -trace_err -trace_screen
@rem for debugging  -trace_msg
@set TRACE_OPTIONS=-trace_err
 
@set LOAD_CALL_RATE=5
@set /A RATE_PERIOD="1000"

