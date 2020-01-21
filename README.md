[![Build Status](https://travis-ci.org/mvysny/webmon.svg?branch=master)](https://travis-ci.org/mvysny/webmon)

# Webmon

A WAR application, deployable to any servlet container, or embeddable to your EAR.
You may receive RSS/e-mail/Jabber notifications of potential problems.
If you cannot use traditional tools such as JConsole or VisualVM then this is for
you.

WebMon requires Java5 or higher.

WebMon can also be embedded in JavaSE applications as well, without the WAR overhead.
Add the `webmon-analyzer` module to your app, and obtain the monitorings as text files
by using telnet, nc or your browser.

Please see the old page at [WebMon](http://baka.sk/webmon/) for further details and screenshots.

WebMon consists of two modules:

* webmon-analyzer - analyzes the JVM it runs in, via the JMX interface. It is able to
  run standalone in any JVM-based deployment including JavaSE, by providing a TCP/IP server
  which produces textual dumps.
* webmon-web - a full-blown WAR application which provides nice charts and graphs,
  allows you to browse class loader hierarchy and files, etc.

In production I recommend to use webmon-analyzer: it is easier to embed to your app,
has very little overhead, and is inherently secure since it listens for incoming connections
on localhost only.

## How to run

### Quickstart / Trying Out

To quickly evaluate WebMon, download webmon-war.war from the [Releases](https://github.com/mvysny/webmon/releases) page.
To run the WAR file, you can for example use the [Jetty Runner](http://wiki.eclipse.org/Jetty/Howto/Using_Jetty_Runner).

The WAR is configured to be started in the context root of /webmon both in JBoss and in Glassfish.

You can also run the webmon-analyzer as standalone, by running

```
java -cp webmon-analyzer-0.16.jar sk.baka.webvm.analyzer.Main
```

Then

```
telnet localhost 5455
```

A dump is made every time when you read that TCP/IP port.
The analyzer then waits for additional commands; just type 'help' to get help.
You can list resources in which jars they are present, and download the resources.

## How To Embed To Your App

To embed into your JavaEE application, just package the WebMon WAR file into your EAR file.

You can also embed WebMon to your JavaSE application, without the web frontend, by
using the `webmon-analyzer` jar file.
webmon-analyzer will open a TCP/IP socket and will provide monitoring information as a simple
plaintext file - you can either use telnet or any browser to download the text report.

To embed WebMon to your app, just add the following Maven dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>sk.baka.webmon</groupId>
  <artifactId>webmon-analyzer</artifactId>
  <version>0.16</version>
</dependency>
```

Then, use the following code to start the WebMon sampler and open the TCP/IP port:

```java
final SamplerConfig cfg = new SamplerConfig(20, 1000, 0);
final IHistorySampler hs = new HistorySampler(cfg, IHistorySampler.HISTORY_PROBLEMS, null, null);
hs.start();
final TCPIPServer webmonServer = new TCPIPServer(5455, 5456, hs);
webmonServer.start();
```

To stop the embedded server:

```java
webmonServer.stop();
hs.stop();
```

You can view the file in your browser at http://localhost:5456 (use ssh port forwarding to access remote machines), or by running

```
nc localhost 5455
```

or by running

```
telnet localhost 5455
```

Example of VM Dump plaintext file: https://github.com/mvysny/webmon/releases/download/webmon-0.15/dump.txt

## Maven 2 users

You can use the following Maven2 repository: http://www.baka.sk/maven2/

Add the following to your pom.xml (for TCP/IP-only analyzer):

```xml
<dependency>
  <groupId>sk.baka.webmon</groupId>
  <artifactId>webmon-analyzer</artifactId>
  <version>0.16</version>
</dependency>
```

Add the following for the web analyzer (war):

```xml
<dependency>
  <groupId>sk.baka.webmon</groupId>
  <artifactId>webmon-web</artifactId>
  <version>0.16</version>
</dependency>
```

