package akskscala.finagle

import org.scalatest.FlatSpec
import java.util.concurrent.TimeUnit

import util.control.Exception._
import java.net.InetSocketAddress
import com.twitter.util.{Future, Duration}
import com.twitter.finagle.builder.Server
import org.scalatest.matchers.ShouldMatchers
import org.jboss.netty.handler.codec.http.{HttpResponseStatus, HttpResponse}
import java.nio.charset.Charset
import com.twitter.finagle.WriteException

class HttpSpec extends FlatSpec with ShouldMatchers {

  "EchoHeaderHttpServer" should "be available" in {
    val address = new InetSocketAddress(9001)
    val server = new EchoHeaderHttpServer(address).start()
    Thread.sleep(1000L) // waiting for server

    ultimately {
      server.close(Duration(1, TimeUnit.MICROSECONDS))
    } apply {
      val clientService = new HttpClient(address).clientService()
      println("))) --- EchoHeaderHttpServer")
      val f1: Future[HttpResponse] = FutureRequester.echoRequest(clientService, "msg 1")
      val f2: Future[HttpResponse] = FutureRequester.echoRequest(clientService, "msg 2")
      val f3: Future[HttpResponse] = FutureRequester.echoRequest(clientService, "") // Empty content causes ChannelClosedException

      Thread.sleep(2000L)
      println("))) This should be printed after '))) Echo req: msg 1'.")

      (f1 join f2 join f3) ensure {
        clientService.release()
      }

      val res1 = f1.get()
      res1.getStatus should equal(HttpResponseStatus.OK)
      res1.getContent.toString(Charset.forName("UTF-8")) should equal("msg 1")

      val res2 = f2.get()
      res2.getStatus should equal(HttpResponseStatus.OK)
      res2.getContent.toString(Charset.forName("UTF-8")) should equal("msg 2")
    }

  }

  "HelloWorldHttpServer" should "be available" in {
    val address = new InetSocketAddress(9002)
    val server: Server = new HelloWorldHttpServer(address).start()
    Thread.sleep(1000L) // waiting for server

    ultimately {
      server.close(Duration(1, TimeUnit.MICROSECONDS))
    } apply {
      val clientService = new HttpClient(address).clientService()
      println("))) --- HelloWorldHttpServer")
      val f1: Future[HttpResponse] = FutureRequester.authorizedRequest(clientService)
      val f2: Future[HttpResponse] = FutureRequester.unauthorizedRequest(clientService)
      (f1 join f2) ensure {
        clientService.release()
      }

      val res1 = f1.get()
      res1.getStatus should equal(HttpResponseStatus.OK)
      res1.getContent.toString(Charset.forName("UTF-8")) should equal("Hello World!")

      intercept[InvalidRequestException] {
        f2.get()
      }
    }

  }

  "Requests" should "not be available when there is no server" in {
    val address = new InetSocketAddress(9003)
    val clientService = new HttpClient(address).clientService()
    println("))) --- no server")
    val f1: Future[HttpResponse] = FutureRequester.authorizedRequest(clientService)
    val f2: Future[HttpResponse] = FutureRequester.unauthorizedRequest(clientService)
    (f1 join f2) ensure {
      clientService.release()
    }

    intercept[WriteException] {
      f1.get()
    }
    intercept[WriteException] {
      f2.get()
    }
  }

}

