package akskscala.finagle

import com.twitter.finagle.Service
import com.twitter.finagle.ServiceFactory
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.stream.{Stream, StreamResponse}

import com.twitter.concurrent.{Observer, Broker, Offer}
import com.twitter.conversions.time._

import java.net.InetSocketAddress
import scala.util.Random

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponseStatus}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpVersion, HttpMethod, DefaultHttpRequest}
import org.jboss.netty.util.CharsetUtil
import com.twitter.util.{Time, Timer, JavaTimer, Future}

/**
 * An example of a streaming server using HTTP Chunking. The Stream
 * Codec uses HTTP Chunks and newline delimited items.
 */
class StreamServer(address: InetSocketAddress) {

  // "tee" messages across all of the registered brokers.
  val addAcceptor = new Broker[Broker[ChannelBuffer]]
  val removeAcceptor = new Broker[Broker[ChannelBuffer]]
  val messages = new Broker[ChannelBuffer]

  private[this] def tee(receivers: Set[Broker[ChannelBuffer]]) {
    Offer.select(
      addAcceptor.recv(broker => tee(receivers + broker)),
      removeAcceptor.recv(broker => tee(receivers - broker)),
      if (receivers.isEmpty) Offer.never
      else {
        messages.recv {
          m => Future.join(receivers map (_ ! m) toSeq) ensure tee(receivers)
        }
      }
    )
  }

  private[this] def produce(r: Random, t: Timer) {
    t.schedule(5.milliseconds.fromNow) {
      val msg = copiedBuffer(Time.now.toLong.toString, CharsetUtil.UTF_8)
      messages.send(msg) andThen produce(r, t)
    }
  }

  // start the two processes.
  tee(Set())
  produce(new Random, new JavaTimer)

  private[this] val service = new Service[HttpRequest, StreamResponse] {

    def apply(req: HttpRequest) = Future {

      val subscriber = new Broker[ChannelBuffer]
      addAcceptor ! subscriber

      new StreamResponse {

        override val httpResponse = new DefaultHttpResponse(req.getProtocolVersion, HttpResponseStatus.OK)

        override def messages = subscriber.recv

        override def error = new Broker[Throwable].recv

        override def release() = {
          removeAcceptor ! subscriber
          // sink any existing messages, so they don't hold up the upstream.
          subscriber.recv foreach (_ => ())
        }

      }
    }
  }

  def start(): Server = {
    ServerBuilder()
      .codec(Stream())
      .bindTo(address)
      .name("StreamServer")
      .build(service)
  }

}

class StreamClient(address: InetSocketAddress) {

  // Construct a ServiceFactory rather than a Client since the TCP Connection
  // is stateful (i.e., messages on the stream even after the initial response).
  val clientServiceFactory: ServiceFactory[HttpRequest, StreamResponse] = ClientBuilder()
    .codec(Stream())
    .hosts(address)
    .tcpConnectTimeout(1.millisecond)
    .hostConnectionLimit(1)
    .buildFactory()

  def start(maxCount: Int): Unit = {
    val req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")

    for {
      clientService <- clientServiceFactory.make()
      streamResponse <- clientService(req)
    } {
      val httpResponse = streamResponse.httpResponse
      if (httpResponse.getStatus.getCode != 200) {
        println(httpResponse.toString)
        clientService.release()
        clientServiceFactory.close()
      } else {
        var observer: Observer = null
        var receiveCount = 0
        observer = streamResponse.channel.respond {
          buffer => {
            receiveCount += 1
            println(">>> Streaming: " + buffer.toString(CharsetUtil.UTF_8))
            if (receiveCount >= maxCount) {
              observer.dispose()
              clientService.release()
              clientServiceFactory.close()
            }
            // We return a Future indicating when we've completed processing the message.
            Future.Done
          }
        }
      }
    }
  }

}
