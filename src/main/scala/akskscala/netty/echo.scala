package akskscala.netty

import java.util.concurrent.Executors
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.bootstrap.{ServerBootstrap, ClientBootstrap}
import socket.nio.{NioServerSocketChannelFactory, NioClientSocketChannelFactory}

/**
 * http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/echo/EchoServer.html
 */
class EchoServer(port: Int = EchoServer.port) {

  // Configure the server.
  private val bootstrap: ServerBootstrap = new ServerBootstrap(
    new NioServerSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()
    )
  )

  def start(): ServerBootstrap = {

    // Set up the pipeline factory.
    bootstrap.setPipelineFactory {
      new ChannelPipelineFactory() {
        override def getPipeline(): ChannelPipeline = {
          return Channels.pipeline(new EchoServerHandler)
        }
      }
    }
    // Bind and start to accept incoming connections.
    bootstrap.bind(new InetSocketAddress(port))

    return bootstrap
  }

  def stop(): Unit = bootstrap.releaseExternalResources()

}


object EchoServer {
  val port = 8001
}

/**
 * http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/echo/EchoServerHandler.html
 */
class EchoServerHandler extends SimpleChannelUpstreamHandler {

  val transferredBytes: AtomicLong = new AtomicLong

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    // Send back the received message to the remote peer.
    transferredBytes.addAndGet(e.getMessage.asInstanceOf[ChannelBuffer].readableBytes)
    e.getChannel.write(e.getMessage)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    // Close the connection when an exception is raised.
    e.getCause.printStackTrace()
    e.getChannel.close()
  }

}

/**
 * http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/echo/EchoClient.html
 */
class EchoClient(host: String = "localhost", port: Int, firstMessageSize: Int = 256) {

  // Configure the client.
  private val bootstrap: ClientBootstrap = new ClientBootstrap(
    new NioClientSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()
    )
  )

  def connect(): Unit = {
    // Set up the pipeline factory.
    bootstrap.setPipelineFactory {
      new ChannelPipelineFactory() {
        override def getPipeline(): ChannelPipeline = {
          return Channels.pipeline(new EchoClientHandler(firstMessageSize))
        }
      }
    }
    // Start the connection attempt.
    future = bootstrap.connect(new InetSocketAddress(host, port))
  }

  private var future: ChannelFuture = null

  def await(): Unit = {
    // Wait until the connection is closed or the connection attempt fails.
    future.getChannel.getCloseFuture.awaitUninterruptibly()
  }

  def disconnect() = {
    // Shut down thread pools to exit.
    bootstrap.releaseExternalResources()
  }

}

/**
 * http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/echo/EchoClientHandler.html
 */
class EchoClientHandler(val firstMessageSize: Int) extends SimpleChannelUpstreamHandler {

  if (firstMessageSize <= 0) {
    throw new IllegalArgumentException("firstMessageSize: " + firstMessageSize)
  }

  private val firstMessage: ChannelBuffer = ChannelBuffers.buffer(firstMessageSize)

  for (i <- 0 until firstMessage.capacity()) {
    firstMessage.writeByte(i.toByte)
  }

  val transferredBytes: AtomicLong = new AtomicLong

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    // Send the first message.  Server will not send anything here because the firstMessage's capacity is 0.
    e.getChannel.write(firstMessage)
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    // Send back the received message to the remote peer.
    transferredBytes.addAndGet(e.getMessage.asInstanceOf[ChannelBuffer].readableBytes)
    e.getChannel.write(e.getMessage)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    //  Close the connection when an exception is raised.
    e.getCause.printStackTrace()
    e.getChannel.close()
  }

}
