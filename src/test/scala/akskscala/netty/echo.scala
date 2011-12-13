package akskscala.netty

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import util.control.Exception._
import concurrent.ops._

/**
 * http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/echo/package-summary.html
 */
class EchoSpec extends FlatSpec with ShouldMatchers {

  "echo example" should "work fine" in {

    val port = EchoServer.port
    val server = new EchoServer(port)

    ultimately {
      server.stop()
      println("An echo server stopped.")
    } apply {
      server.start()
      println("An echo server started.")
      val client = new EchoClient(port = port)
      client.connect()
      println("An client which awaits the server is connected.")
      println("")
      println("Now try 'telnet localhost " + port + "'!")
      println("")
      spawn {
        client.await()
      }
      for (i <- 1 to 5) {
        Thread.sleep(2000L)
        print(".")
      }
      client.disconnect()
      println("... The client is disconnected.")
    }

  }

}
