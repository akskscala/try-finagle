package akskscala.finagle

import org.scalatest.FlatSpec
import util.control.Exception._
import java.util.concurrent.TimeUnit
import com.twitter.util.Duration
import java.net.InetSocketAddress

class StreamSpec extends FlatSpec {

  "Stream" should "be available" in {
    val address = new InetSocketAddress(9201)
    val server = new StreamServer(address).start()
    Thread.sleep(1000L) // waiting for server

    ultimately {
      server.close(Duration(1, TimeUnit.MICROSECONDS))
    } apply {
      new StreamClient(address).start(20)
      Thread.sleep(200L)
    }

  }

}