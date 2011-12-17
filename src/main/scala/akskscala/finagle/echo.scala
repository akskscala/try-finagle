package akskscala.finagle

import java.net.InetSocketAddress

import com.twitter.finagle.{Codec, CodecFactory}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.Service
import com.twitter.util.Future
import org.jboss.netty.handler.codec.string.{StringEncoder, StringDecoder}
import org.jboss.netty.handler.codec.frame.{Delimiters, DelimiterBasedFrameDecoder}
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.channel.{ChannelPipeline, Channels, ChannelPipelineFactory}

class EchoServer(address: InetSocketAddress) {

  private[this] val service = new Service[String, String] {
    def apply(req: String): Future[String] = Future.value(req)
  }

  // when call `build(Service)`, start listening on the port
  def start(): Server = ServerBuilder()
    .codec(StringCodec)
    .bindTo(address)
    .name("EchoServer")
    .build(service)

}

class EchoClient(address: InetSocketAddress) {

  def clientService(): Service[String, String] = ClientBuilder()
    .codec(StringCodec)
    .hosts(address)
    .hostConnectionLimit(1)
    .build()

}

/**
 * http://docs.jboss.org/netty/3.2/api/org/jboss/netty/channel/ChannelPipeline.html
 * http://docs.jboss.org/netty/3.2/guide/html/start.html#start.pojo
 */
object StringCodec extends CodecFactory[String, String] {

  override def server = Function.const {
    new Codec[String, String] {
      override def pipelineFactory = new ChannelPipelineFactory {
        override def getPipeline(): ChannelPipeline = {
          val pipeline = Channels.pipeline()
          pipeline.addLast("line", new DelimiterBasedFrameDecoder(100, Delimiters.lineDelimiter: _*))
          pipeline.addLast("decode", new StringDecoder(CharsetUtil.UTF_8))
          pipeline.addLast("encode", new StringEncoder(CharsetUtil.UTF_8))
          pipeline
        }
      }
    }
  }

  override def client = Function.const {
    new Codec[String, String] {
      override def pipelineFactory = new ChannelPipelineFactory {
        override def getPipeline(): ChannelPipeline = {
          val pipeline = Channels.pipeline()
          pipeline.addLast("encode", new StringEncoder(CharsetUtil.UTF_8))
          pipeline.addLast("decode", new StringDecoder(CharsetUtil.UTF_8))
          pipeline
        }
      }
    }
  }

}
