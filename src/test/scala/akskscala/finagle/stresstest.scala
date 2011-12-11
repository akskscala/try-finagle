package akskscala.finagle

import org.scalatest.FlatSpec
import java.net.InetSocketAddress
import util.control.Exception._
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import com.twitter.util.{Time, MapMaker, Future, Duration}
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpVersion, DefaultHttpRequest, HttpResponseStatus}

class StressTestExecutor extends FlatSpec {

  "StressTest" should "be available" in {
    val address = new InetSocketAddress(9301)
    val server = new EchoHeaderHttpServer(address).start()
    Thread.sleep(1000L) // waiting for server

    ultimately {
      server.close(Duration(1, TimeUnit.MICROSECONDS))
    } apply {

      val clientService = new HttpClient(address).clientService()

      val responseMap = MapMaker[HttpResponseStatus, AtomicInteger] {
        config => config.compute(k => new AtomicInteger(0))
      }

      val totalRequests: Int = 500
      val concurrencyNum: Int = 10

      val errorCount = new AtomicInteger(0)
      val completedCount = new AtomicInteger(0)
      val req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
      req.addHeader("echo", "foo")

      val futures: Seq[Future[Unit]] = Future.parallel(concurrencyNum) {
        Future.times(totalRequests / concurrencyNum) {
          clientService(req) onSuccess {
            response => responseMap(response.getStatus).incrementAndGet()
          } handle {
            case e => {
              e.printStackTrace()
              errorCount.incrementAndGet()
            }
          } ensure {
            completedCount.incrementAndGet()
          }
        }
      }

      val startTime = Time.now
      Future.join(futures) ensure {

        clientService.release()

        val duration = startTime.untilNow
        println("---------------------------------")
        println("%20s\t%s".format("Status", "Count"))
        for ((status, stat) <- responseMap) {
          println("%20s\t%d".format(status, stat.get))
        }
        println("---------------------------------")
        println("%d requests completed in %dms".format(completedCount.get, duration.inMilliseconds))
        println("(%.3f requests/second)".format(totalRequests.toFloat / duration.inMillis.toFloat * 1000))
        println("%d errors".format(errorCount.get))
        println("---------------------------------")
      }
      Thread.sleep(2000L)

    }

  }

}