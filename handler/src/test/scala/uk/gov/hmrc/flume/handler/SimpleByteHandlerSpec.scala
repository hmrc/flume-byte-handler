package uk.gov.hmrc.flume.handler

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest

import org.apache.flume.source.http.HTTPBadRequestException
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}

class SimpleByteHandlerSpec extends WordSpecLike with Matchers with MockitoSugar {

  val handler = new SimpleByteHandler
  val content100: Stream[Byte] = Stream.fill(100)(5.toByte)

  "a request with correct content length" should {
    "result in a single event" in {
      validateLengthAndContent(100, 100, content100)
    }
  }

  "a request with no content length" should {
    "result in a single event" in {
      validateLengthAndContent(-1, 100, content100)
    }
  }

  "a request with a content length which is too short" should {
    "result in a single event" in {
      validateLengthAndContent(50, 100, content100)
    }
  }

  "a request with a content length which is too long" should {
    "result in a single event" in {
      validateLengthAndContent(150, 100, content100)
    }
  }

  "a request with a 1Mb content and 500kb content length" should {
    "throw an HTTPBadRequestException after reading the content stream" in {
      val kb500 = 500000
      val mb1 = 1000000
      val content: Stream[Byte] = Stream.fill(mb1)(5.toByte)
      val request = mock[HttpServletRequest]

      when(request.getContentLength).thenReturn(kb500)
      when(request.getInputStream).thenReturn(servletStream(content))

      an[HTTPBadRequestException] should be thrownBy handler.getEvents(request)

      verify(request).getInputStream
    }
  }

  "a request with a 1mb content length" should {
    "throw an HTTPBadRequestException without reading the content stream" in {
      val mb1 = 1000000
      val request = mock[HttpServletRequest]

      when(request.getContentLength).thenReturn(mb1)

      an [HTTPBadRequestException] should be thrownBy handler.getEvents(request)

      verify(request, never()).getInputStream
    }
  }

  def validateLengthAndContent(suggestedLength: Int, expectedLength: Int, content: Stream[Byte]): Unit = {
    val request = mock[HttpServletRequest]

    when(request.getContentLength).thenReturn(suggestedLength)
    when(request.getInputStream).thenReturn(servletStream(content))

    val events = handler.getEvents(request)

    events should have length 1
    events.get(0).getBody should have length expectedLength
    events.get(0).getBody shouldBe content.toArray
  }

  def servletStream(contents: Stream[Byte]): ServletInputStream = {
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
