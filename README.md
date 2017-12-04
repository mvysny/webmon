[![Build Status](https://travis-ci.org/mvysny/webmon.svg?branch=master)](https://travis-ci.org/mvysny/webmon)

# Webmon

A WAR application, deployable to any servlet container/embeddable to your EAR. You may receive RSS/e-mail/Jabber notifications of potential problems. If you cannot use traditional tools such as JConsole or VisualVM then this is for you. WebMon requires Java5 or higher.

WebMon can be embedded in JavaSE applications as well, without the WAR overhead.

Please see http://baka.sk/webmon/ for further details and screenshots.

## How to run

Download webmon-war.war from the Downloads page. To evaluate, you can use Jetty Runner http://wiki.eclipse.org/Jetty/Howto/Using_Jetty_Runner

The WAR is configured to be started in the context root of /webmon both in JBoss and in Glassfish.

You can also run the standalone TCP-IP server, by running

```
java -cp webmon-analyzer-0.16.jar sk.baka.webvm.analyzer.Main
```

Then

```
telnet localhost 5455
```

A dump is made, and the analyzer waits for additional commands, just type 'help' to get help. You can list resources in which jars they are present, and download the resources.


## How to embed

To embed into your JavaEE application, just embed it into your EAR.

You can also embed webmon to your non-JavaEE application - in this case webmon will open a socket and will provide monitoring information as a simple plaintext file - you can use any browser to download the file. Download webmon-analyzer.jar. To embed webmon to your app, just use the following code:

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

