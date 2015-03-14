# Webmon

A WAR application, deployable to any servlet container/embeddable to your EAR. You may receive RSS/e-mail/Jabber notifications of potential problems. If you cannot use traditional tools such as JConsole or VisualVM then this is for you. WebMon requires Java5 or higher.

WebMon can be embedded in JavaSE applications as well, without the WAR overhead.

Please see http://baka.sk/webmon/ for further details and screenshots.

## How to run

Download webmon-war.war from the Downloads page. To evaluate, you can use Jetty Runner http://wiki.eclipse.org/Jetty/Howto/Using_Jetty_Runner

The WAR is configured to be started in the context root of /webmon both in JBoss and in Glassfish.

You can also run the standalone TCP-IP server, by running

```
java -cp webmon-analyzer-0.15.jar sk.baka.webvm.analyzer.utils.TCPIPServer
```

Then

```
telnet localhost 50000
```

A dump is made, and the analyzer waits for additional commands, just type 'help' to get help. You can list resources in which jars they are present, and download the resources.


## How to embed

To embed into your JavaEE application, just embed it into your EAR.

You can also embed webmon to your non-JavaEE application - in this case webmon will open a socket and will provide monitoring information as a simple plaintext file - you can use any browser to download the file. Download webmon-analyzer.jar. To embed webmon to your app, just use the following code:

```
final SamplerConfig cfg = new SamplerConfig(20, 1000, 0);
final IHistorySampler hs = new HistorySampler(cfg, IHistorySampler.HISTORY_PROBLEMS, null, null);
hs.start();
final TCPIPServer webmonServer = new TCPIPServer(WEBMON_PORT, hs);
webmonServer.start();
```

To stop the embedded server:

```
webmonServer.stop();
hs.stop();
```

You can view the file in your browser at http://host:WEBMON_PORT (for some reason the browser will not download the entire file contents though), or by running

```
nc host WEBMON_PORT|less
```

or by running

```
telnet host WEBMON_PORT|less
```

Example of VM Dump plaintext file: http://webmon.googlecode.com/files/webmon.txt

## Maven 2 users

You can use the following Maven2 repository: http://www.baka.sk/maven2/

Add the following to your pom.xml (for TCP/IP-only analyzer):

```
<dependency>
  <groupId>sk.baka.webmon</groupId>
  <artifactId>webmon-analyzer</artifactId>
  <version>0.12</version>
</dependency>
```

Add the following for the web analyzer (war):

```
<dependency>
  <groupId>sk.baka.webmon</groupId>
  <artifactId>webmon-web</artifactId>
  <version>0.12</version>
</dependency>
```

