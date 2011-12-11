package akskscala.finagle

import org.scalatest.FlatSpec
import util.control.Exception._
import java.util.concurrent.TimeUnit
import com.twitter.util.Duration
import java.net.InetSocketAddress
import org.scalatest.matchers.ShouldMatchers

class EchoSpec extends FlatSpec with ShouldMatchers {

  "Echo" should "be available" in {
    val address = new InetSocketAddress(9101)
    val server = new EchoServer(address).start()
    Thread.sleep(1000L) // waiting for server

    ultimately {
      server.close(Duration(1, TimeUnit.MICROSECONDS))
    } apply {

      val clientService = new EchoClient(address).clientService()
      val future = clientService("My First Finagle App!\n") onSuccess {
        result => println("))) Received result asynchronously: " + result)
      } onFailure {
        error => error.printStackTrace()
      } ensure {
        clientService.release()
      }

      val response = future.get()
      response should equal("My First Finagle App!")
    }

  }

}