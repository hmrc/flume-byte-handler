/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.flume.handler

import java.util
import javax.servlet.http.HttpServletRequest

import org.apache.flume.event.EventBuilder
import org.apache.flume.source.http.{HTTPBadRequestException, HTTPSourceHandler}
import org.apache.flume.{Context, Event}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * High performance handler that just converts each HTTP request body into a
  * single event.
  */
class SimpleByteHandler extends HTTPSourceHandler {

  val bufferLength = 2048
  val maxBytes = 500000

  override def getEvents(request: HttpServletRequest): util.List[Event] = {
    val length = contentLength(request)
    val arrayBuilder = mutable.ArrayBuilder.make[Byte]
    arrayBuilder.sizeHint(length)

    Seq[Event](consumeRequest(request, arrayBuilder)).asJava
  }

  override def configure(context: Context): Unit = {
    // this handler has no configuration, so do nothing here
  }

  def contentLength(request: HttpServletRequest): Int = {
    // get the content length if available
    var length = request.getContentLength
    if (length == -1) { length = 0 }

    if (length > maxBytes) {
      throw new HTTPBadRequestException(s"Event content length too large (> $maxBytes bytes). Aborting.")
    }

    length
  }

  def consumeRequest(request: HttpServletRequest, arrayBuilder: mutable.ArrayBuilder[Byte]): Event = {
    /*
     * Attempts to efficiently allocate an underlying byte array and read the
     * stream contents using the content length as a hint (but not relying on it).
     */
    val stream = request.getInputStream
    val buffer: Array[Byte] = new Array[Byte](bufferLength)
    var bytesRead = 0
    var readLength = stream.read(buffer, 0, bufferLength)

    while (readLength != -1 && bytesRead < maxBytes) {
      bytesRead = bytesRead + readLength
      if (bufferLength == readLength)
        arrayBuilder ++= mutable.WrappedArray.make(buffer)
      else
        arrayBuilder ++= mutable.WrappedArray.make(buffer.slice(0, readLength))

      readLength = stream.read(buffer, 0, bufferLength)
    }

    if (bytesRead > maxBytes) {
      throw new HTTPBadRequestException(s"Event content too large (> $maxBytes bytes)")
    }

    EventBuilder.withBody(arrayBuilder.result)
  }
}
