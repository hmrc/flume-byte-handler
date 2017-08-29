package uk.gov.hmrc.flume.handler

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest

import org.apache.flume.Event
import org.mortbay.jetty.Request
import org.scalameter.api._
import org.scalameter.picklers.Implicits.doublePickler

import scala.collection.mutable

abstract class SimpleByteHandlerPerfSpec extends Bench[Double] {
  def measurer: Measurer[Double] = null
  def executor: Executor[Double] = null
  def reporter = new LoggingReporter[Double]
  def persistor = Persistor.None

  val contentLengths: Gen[Int] = Gen.range("content-length")(50000, 500000, 45000)
  val requests: Gen[HttpServletRequest] = for {
    length <- contentLengths
  } yield build(length, Stream.fill(length)(5.toByte))

  def build(contentLength: Int, contents: Stream[Byte]): HttpServletRequest = {
    new Request {
      override def getContentLength: Int = {
        contentLength
      }

      override def getInputStream: ServletInputStream = {
        new ServletInputStream {
          val iterator: Iterator[Byte] = contents.iterator
          override def read(): Int = {
            if (iterator.hasNext) {
              iterator.next
            } else {
              -1
            }
          }
        }
      }
    }
  }
}

object SimpleByteHandlerThroughputSpec extends SimpleByteHandlerPerfSpec {
  override def measurer = new Measurer.Default
  override def executor = SeparateJvmsExecutor(
    new Executor.Warmer.Default,
    Aggregator.min[Double],
    measurer
  )

  def handler = new SimpleByteHandler

  performance of "HandlerThroughput" in {
    measure method "getEvents" config (
      exec.benchRuns -> 20
      ) in {
      using(requests) in {
        request => handler.getEvents(request)
      }
    }
  }
}

object SimpleByteHandlerMemorySpec extends SimpleByteHandlerPerfSpec {
  override def measurer = new Measurer.MemoryFootprint
  override def executor = LocalExecutor(
    new Executor.Warmer.Default,
    Aggregator.max,
    measurer
  )

  // overridden to hold references to buffers, to get a better measurement
  def handler = new MemoryTestHandler
  class MemoryTestHandler extends SimpleByteHandler {
    def buildEventMemory(request: HttpServletRequest): Result = {
      val length = contentLength(request)
      val arrayBuilder = mutable.ArrayBuilder.make[Byte]
      arrayBuilder.sizeHint(length)

      Result(arrayBuilder, consumeRequest(request, arrayBuilder))
    }
  }

  case class Result(
    arrayBuilder: mutable.ArrayBuilder[Byte],
    event: Event
  )

  performance of "HandlerMemoryUsage" in {
    measure method "getEvents" config (
      exec.benchRuns -> 3
      ) in {
      using(requests) in {
        request => handler.buildEventMemory(request)
      }
    }
  }
}
