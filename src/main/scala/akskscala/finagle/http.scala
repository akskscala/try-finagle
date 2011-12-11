package akskscala.finagle

import java.net.InetSocketAddress

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.util.CharsetUtil.UTF_8

import com.twitter.finagle.builder.{Server, ServerBuilder, ClientBuilder}
import com.twitter.finagle.http.Http
import com.twitter.util.Future
import com.twitter.finagle.{Service, SimpleFilter}

class InvalidRequestException extends Exception

class EchoHeaderHttpServer(address: InetSocketAddress) {

  type Req = HttpRequest
  type Rep = HttpResponse

  def start(): Server = {
    val echoHeaderService = new Service[Req, Rep] {
      def apply(req: Req): Future[Rep] = {
        val response = new DefaultHttpResponse(HTTP_1_1, OK)
        val toEcho = req.getHeader("echo")
        val content = if (toEcho != null) toEcho.getBytes() else "no content".getBytes()
        response.setContent(copiedBuffer(content))
        Future.value(response)
      }
    }
    ServerBuilder()
      .codec(Http())
      .bindTo(address)
      .name("EchoHeaderHttpServer")
      .build(echoHeaderService)
  }

}

class HelloWorldHttpServer(address: InetSocketAddress) {

  import HttpServerFilters._

  type Req = HttpRequest
  type Rep = HttpResponse

  def start(): Server = ServerBuilder()
    .codec(Http())
    .bindTo(address)
    .name("HelloWorldHttpServer")
    .build(new ErrorHandler andThen new Authorizer andThen new HelloWorldResponder)

}

object HttpServerFilters {

  type Req = HttpRequest
  type Rep = HttpResponse

  class ErrorHandler extends SimpleFilter[Req, Rep] {
    def apply(req: Req, service: Service[Req, Rep]): Future[Rep] = {
      service(req) handle {
        case error => {
          val statusCode = error match {
            case _: IllegalArgumentException => FORBIDDEN
            case _ => INTERNAL_SERVER_ERROR
          }
          val errorResponse = new DefaultHttpResponse(HTTP_1_1, statusCode)
          errorResponse.setContent(copiedBuffer(error.getStackTraceString, UTF_8))
          errorResponse
        }
      }
    }
  }

  class Authorizer extends SimpleFilter[Req, Rep] {
    def apply(req: Req, service: Service[Req, Rep]): Future[Rep] = {
      req.getHeader("Authorization") match {
        case "open sesame" => service(req)
        case _ => Future.exception(new IllegalArgumentException("You don't know the secret"))
      }
    }
  }

  class HelloWorldResponder extends Service[Req, Rep] {
    def apply(req: Req): Future[Rep] = {
      val response = new DefaultHttpResponse(HTTP_1_1, OK)
      response.setContent(copiedBuffer("Hello World!", UTF_8))
      Future.value(response)
    }
  }

}

class HttpClient(address: InetSocketAddress) {

  import HttpClientFilters._

  type Req = HttpRequest
  type Rep = HttpResponse

  def clientService(): Service[Req, Rep] = {
    val service = ClientBuilder()
      .codec(Http())
      .hosts(address)
      .hostConnectionLimit(10)
      .build()
    new ErrorHandler andThen service
  }

}

object HttpClientFilters {

  type Req = HttpRequest
  type Rep = HttpResponse

  class ErrorHandler extends SimpleFilter[Req, Rep] {
    def apply(req: Req, service: Service[Req, Rep]): Future[Rep] = {
      service(req) flatMap {
        response => {
          response.getStatus match {
            case OK => Future.value(response)
            case FORBIDDEN => Future.exception(new InvalidRequestException)
            case _ => Future.exception(new Exception(response.getStatus.getReasonPhrase))
          }
        }
      }
    }
  }

}

object FutureRequester {

  type Req = HttpRequest
  type Rep = HttpResponse

  def echoRequest(service: Service[Req, Rep], echo: String): Future[Rep] = {
    val req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
    req.addHeader("echo", echo)
    service(req) onSuccess {
      rep => {
        val responseString = rep.getContent.toString(CharsetUtil.UTF_8)
        println("))) Echo req: " + responseString)
      }
    } onFailure {
      error => println("))) Echo req errored: " + error.getClass.getName)
    }
  }

  def authorizedRequest(service: Service[Req, Rep]): Future[Rep] = {
    val req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
    req.addHeader("Authorization", "open sesame")
    service(req) onSuccess {
      rep => {
        val responseString = rep.getContent.toString(CharsetUtil.UTF_8)
        println("))) Authorized req: " + responseString)
      }
    } onFailure {
      error => println("))) Authorized req errored: " + error.getClass.getName)
    }
  }

  def unauthorizedRequest(service: Service[Req, Rep]): Future[Rep] = {
    val req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
    service(req) onSuccess {
      rep => {
        val responseString = rep.getContent.toString(CharsetUtil.UTF_8)
        println("))) Unauthorized req: " + responseString)
      }
    } onFailure {
      error => println("))) Unauthorized req errored (as desired): " + error.getClass.getName)
    }
  }

}