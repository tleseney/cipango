#!/bin/sh

. "./setEnv.sh"

${SIPP} ${AS} -m ${NB_LOAD_MESSAGE} -r ${LOAD_CALL_RATE} -rp ${RATE_PERIOD} \
          ${SIPP_OPTIONS} -i ${SIPP_UAC_IP} -p ${UAC_PORT} -sf ../message.xml ${TRACE_OPTIONS} \
          ${UAC_SPECIFIC_OPTIONS} -s uas $@

