
# flume-byte-handler

[![Build Status](https://travis-ci.org/hmrc/flume-byte-handler.svg?branch=master)](https://travis-ci.org/hmrc/flume-byte-handler) [ ![Download](https://api.bintray.com/packages/hmrc/releases/flume-byte-handler/images/download.svg) ](https://bintray.com/hmrc/releases/flume-byte-handler/_latestVersion)

This is a high performance handler for use with the Flume HttpSource. It does no
processing of the HTTP request beyond copying the byte payload into a single
Flume event. It is also coded to support large message payloads by minimising
the amount of memory allocated for each event, and pre-allocating the correct
event memory size based on the HTTP Content-Length header value (where this is
available).

## Configuration

To use this handler, you must specify its class in the HttpSource in your Flume
configuration file. An example of this is :

```properties
audit.sources = httpSource
audit.channels = memoryChannel
audit.sinks = nullSink

# Source for accepting HTTP calls
audit.sources.httpSource.type = http
audit.sources.httpSource.channels = memoryChannel
audit.sources.httpSource.port = 9001
audit.sources.httpSource.handler = uk.gov.hmrc.flume.handler.SimpleByteHandler

# Provides an in-memory channel
audit.channels.memoryChannel.type = memory

# Null Sink
audit.sinks.nullSink.type = null
audit.sinks.nullSink.channel = memoryChannel
```

## Building

To run unit tests, you can use the usual SBT command ```sbt test```.

To run some simple ScalaMeter benchmark tests, us ```sbt bench:test```.

Gatling tests need a running Flume process, which can use the configuration
above. The assembly binary will need to be included in the Flume path for this
to work correctly, as it contains the Scala dependencies needed by the handler
code. Run the tests using ```sbt gatling-it:test```.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    