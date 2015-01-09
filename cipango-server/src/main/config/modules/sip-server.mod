#
# Base Cipango Server Module
#

[depend]
server

[lib]
lib/sip/cipango-server-*.jar
lib/sip/cipango-util-*.jar
lib/sip/cipango-sip-*.jar
lib/sip/cipango-dns-*.jar
lib/sip/sip-api-1.1.jar

[xml]
etc/cipango.xml

[ini-template]
sip.port=5060
# sip.host=localhost
sip.mtu=65536



