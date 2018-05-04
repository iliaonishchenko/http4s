package org.http4s
package client
package middleware

import cats.effect._
import fs2.io.readInputStream
import org.http4s.dsl.io._
import scala.io.Source

/**
  * Common Tests for Logger, RequestLogger, and ResponseLogger
  */
class LoggerSpec extends Http4sSpec {

  val testApp = HttpApp[IO] {
    case GET -> Root / "request" =>
      Ok("request response")
    case req @ POST -> Root / "post" =>
      Ok(req.body)
    case _ =>
      NotFound()
  }

  def testResource = getClass.getResourceAsStream("/testresource.txt")

  def body: EntityBody[IO] = readInputStream[IO](IO.pure(testResource), 4096)

  val expectedBody: String = Source.fromInputStream(testResource).mkString

  "ResponseLogger" should {
    val responseLoggerService = ResponseLogger(true, true)(testApp)

    "not affect a Get" in {
      val req = Request[IO](uri = uri("/request"))
      responseLoggerService(req) must returnStatus(Status.Ok)
    }

    "not affect a Post" in {
      val req = Request[IO](uri = uri("/post"), method = POST).withBodyStream(body)
      val res = responseLoggerService(req)
      res must returnStatus(Status.Ok)
      res must returnBody(expectedBody)
    }
  }

  "RequestLogger" should {
    val requestLoggerService = RequestLogger(true, true)(testApp)

    "not affect a Get" in {
      val req = Request[IO](uri = uri("/request"))
      requestLoggerService(req) must returnStatus(Status.Ok)
    }

    "not affect a Post" in {
      val req = Request[IO](uri = uri("/post"), method = POST).withBodyStream(body)
      val res = requestLoggerService(req)
      res must returnStatus(Status.Ok)
      res must returnBody(expectedBody)
    }
  }

  "Logger" should {
    val loggerApp =
      Logger(true, true)(Client.fromHttpApp(testApp)).toHttpApp

    "not affect a Get" in {
      val req = Request[IO](uri = uri("/request"))
      loggerApp(req) must returnStatus(Status.Ok)
    }

    "not affect a Post" in {
      val req = Request[IO](uri = uri("/post"), method = POST).withBodyStream(body)
      val res = loggerApp(req)
      res must returnStatus(Status.Ok)
      res must returnBody(expectedBody)
    }
  }
}
