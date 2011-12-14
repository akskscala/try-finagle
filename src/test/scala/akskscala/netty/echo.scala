package akskscala.netty

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import util.control.Exception._
import concurrent.ops._
import java.io.{InputStreamReader, BufferedReader, BufferedInputStream}
import java.net.{SocketException, InetSocketAddress, Socket}

import resource._
import java.security.SecureRandom

/**
 * http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/echo/package-summary.html
 */
class SocketEchoSpec extends FlatSpec with ShouldMatchers {

  "Using java.net.Socket" should "work fine" in {

    val port = 8101
    val server = new EchoServer(port)

    ultimately {
      server.stop()
      println("Server stopped.")
    } apply {
      server.start()
      println("Server started.")

      val socket = new Socket("localhost", port)
      spawn {
        // to shutdown client
        Thread.sleep(10000L)
        socket.close()
      }
      for {
        socket <- managed(socket)
        in <- managed(socket.getInputStream)
        out <- managed(socket.getOutputStream)
      } {
        spawn {
          while (true) {
            Thread.sleep(100L * new SecureRandom().nextInt(10))
            out.write((("Something is technically wrong.\n").getBytes()))
          }
        }
        for (reader <- managed(new BufferedReader(new InputStreamReader(in)))) {
          var line = ""
          ignoring(classOf[SocketException]) {
            var line = reader.readLine
            while (line != null) {
              println(line)
              line = reader.readLine
            }
          }
        }
      }
    }
  }

}

class EchoSpec extends FlatSpec with ShouldMatchers {

  "echo example" should "work fine" in {

    val port = 8102
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