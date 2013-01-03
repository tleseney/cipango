Cipango is a SIP Servlets extension to the popular Jetty HTTP Servlet
engine. Cipango/Jetty is then a convergent SIP/HTTP Application Server
compliant with both SIP Servlets 1.1 and HTTP Servlets 2.5 standards.
It also features a Diameter extension to develop IMS applications.

Cipango shares many of Jetty goals and is designed to be simple to
use, flexible and highly performant. It can be used for convergent
HTTP/SIP applications and is also able to support any kind of SIP-only
applications such as :

* Subscriber communications applications : presence, call forward and
  screening, rich-media services, conferencing, instant messaging,
  contact centers...

* Network functions : rich call control access, protocol translation,
  intelligent routing and logging...

The latest version is at https://github.com/cipango/cipango

To build, use:

  mvn clean install

The cipango distribution will be built in

  cipango-distribution/target/

The first build may take a long time as Maven downloads all the
dependencies. You can bypass tests by building with -Dmaven.test.skip=true.
This version of Cipango relies on Jetty 9.0.0.M5, available at
git://git.eclipse.org/gitroot/jetty/org.eclipse.jetty.project.git

For more information including use and installation instructions see
http://confluence.cipango.org/display/DOC/Cipango+Documentation
