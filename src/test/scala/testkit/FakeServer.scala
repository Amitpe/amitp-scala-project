package testkit

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model._
import com.wix.e2e.http.RequestHandler
import com.wix.e2e.http.client.sync._
import com.wix.e2e.http.server.WebServerFactory._
import json.JsonJacksonMarshaller

import scala.collection.immutable

class FakeServer(port: Int,
                 token: String,
                 channels: Seq[String]) {
  type ErrorCode = String
  implicit private val mapper = new JsonJacksonMarshaller

  val server = aStubWebServer
    .onPort(port)
    .build

  private val handler: RequestHandler = {
    case HttpRequest(_, _, headers, _, _) if invalidToken(headers) =>
      handleInvalidTokenRequest()
    case HttpRequest(HttpMethods.POST, Path("/api/chat.postMessage"), _, entity, _) =>
      handlePostMessageToChannelRequest(entity)
    case _ =>
      handleUnknownRequest()
  }

  private def invalidToken(headers: immutable.Seq[HttpHeader]) =
    !isAuthenticated(headers)

  private def isAuthenticated(headers: immutable.Seq[HttpHeader]) =
    headers.contains(Authorization(OAuth2BearerToken(token)))

  def handleInvalidTokenRequest() =
    anErrorResponseWith("not_authed")

  def handlePostMessageToChannelRequest(entity: RequestEntity): HttpResponse =
    validateRequestBody(entity) match {
      case Some(errorCode) => anErrorResponseWith(errorCode)
      case _ => aValidResponse()
    }

  private def validateRequestBody(entity: RequestEntity): Option[ErrorCode] = {
    val requestBody = entity.extractAs[FakeRequest]
    if (!channels.contains(requestBody.property))
      Some("not_found")
    else
      None
  }

  private def handleUnknownRequest() =
    aNotFoundResponse

  private def anErrorResponseWith(error: String) =
    HttpResponse(
      status = StatusCodes.OK,
//      entity = HttpEntity(mapper.marshall(PostMessageToChannelResponse(ok = false, error = error)))
    )

  private def aValidResponse() =
    HttpResponse(
      status = StatusCodes.OK,
//      entity = HttpEntity(mapper.marshall(PostMessageToChannelResponse(ok = true, error = null)))
    )

  private def aNotFoundResponse: HttpResponse =
    HttpResponse(
      status = StatusCodes.NotFound,
      entity = HttpEntity(s"Request's path is not supported in slack Test-kit.")
    )

  server.appendAll(handler)
}

case class FakeRequest(property: String)