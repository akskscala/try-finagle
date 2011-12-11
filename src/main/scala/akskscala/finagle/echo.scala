package akskscala.finagle

import java.net.InetSocketAddress

import com.twitter.finagle.{Codec, CodecFactory}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.Service
import com.twitter.util.Future
import org.jboss.netty.handler.codec.string.{StringEncoder, StringDecoder}
import org.jboss.netty.channel.{Channels, ChannelPipelineFactory}
import org.jboss.netty.handler.codec.frame.{Delimiters, DelimiterBasedFrameDecoder}
import org.jboss.netty.util.CharsetUtil

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

object StringCodec extends CodecFactory[String, String] {

  // TODO

  override def server = Function.const {
    new Codec[String, String] {
      def pipelineFactory = new ChannelPipelineFactory {
        def getPipeline = {
          val pipeline = Channels.pipeline()
          pipeline.addLast("line", new DelimiterBasedFrameDecoder(100, Delimiters.lineDelimiter: _*))
          pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8))
          pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8))
          pipeline
        }
      }
    }
  }

  override def client = Function.const {
    new Codec[String, String] {
      def pipelineFactory = new ChannelPipelineFactory {
        def getPipeline = {
          val pipeline = Channels.pipeline()
          pipeline.addLast("stringEncode", new StringEncoder(CharsetUtil.UTF_8))
          pipeline.addLast("stringDecode", new StringDecoder(CharsetUtil.UTF_8))
          pipeline
        }
      }
    }
  }

}
