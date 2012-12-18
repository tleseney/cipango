To use UNIX load script:
1) install and compile SIPp http://sipp.sourceforge.net
2) adapt in setEnv.sh the property SIPP_HOME
3) start cipango with the servlet application load-sipapp
4) start the script UAS: ./uas.sh
5) start the script UAC: 
   - in mode proxy: ./uac.sh -s proxy
   - in mode B2B UA: ./uac.sh -s b2b
   - in mode proxy: ./uac.sh -s uas