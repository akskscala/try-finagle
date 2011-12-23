package akskscala.twitterutil

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import com.twitter.util.{Future, Try, Return, Throw}
import java.lang.RuntimeException
import scala.util.control.Exception._

/**
 * @see https://github.com/twitter/util/blob/master/util-core/src/test/scala/com/twitter/util/FutureSpec.scala
 */
class UsingFutureSpec extends WordSpec with ShouldMatchers {

  "Future" should {

    def sayHello(input: String): String = "Hello, " + input + "!"
    val f1 = Future.value(sayHello("World")) onSuccess {
      result => println("Success!: " + result)
    } onFailure {
      t => println("Failed!: " + t.getLocalizedMessage)
    }
    val successCallback = (result: String) => println("Success!: " + result)
    val failureCallback = (t: Throwable) => println("Failed!: " + t.getLocalizedMessage)
    val f2 = Future.value(sayHello("Twitter")) onSuccess successCallback onFailure failureCallback

    "work fine with onSuccess" in {
      Future.value("foo") onSuccess {
        result => result should equal("foo")
      }
    }

    "work fine with onFailure" in {
      // thrown exception
      def throwsException() = throw new RuntimeException
      ignoring(classOf[Throwable]) {
        Future.value(throwsException()) onFailure {
          t => fail("onFailure should not called.")
        }
      }
      // not thrown but passed exception
      Future.exception(new RuntimeException) onSuccess {
        _ => fail("The iteration should fail!")
      } onFailure {
        t => println("onFailure is called: " + t)
      }
      // - the following code work same.
      // - TODO: what is Future.tracer??
      // Future.rawException(new RuntimeException) onSuccess {
    }

    "work fine when using Future.whileDo" in {
      var i = 0
      val whileDoFuture: Future[Unit] = Future.whileDo(i < 3) {
        i += 1
        Thread.sleep(1000L)
        println("a" * i)
        Future()
      }
      whileDoFuture onSuccess {
        _ => println("Futures by whileDo completed successfully!")
      }
      i = 0
      whileDoFuture onSuccess {
        _ => println("Futures seems to be empty..")
      }
    }

    "work fine when using Future.join" in {
      val joinedFuture: Future[Unit] = Future.join(Seq(f1, f2))
      joinedFuture foreach {
        _: Unit => println("Futures all completed successfully!")
      }
    }

    "work fine when using Future.select" in {
      val selectedFuture: Future[(Try[String], Seq[Future[String]])] = Future.select(Seq(f1, f2))
      selectedFuture.foreach {
        case (selected, tail) => {
          selected() should equal("Hello, World!")
          Future.select(tail) foreach {
            case (next, _) => next() should equal("Hello, Twitter!")
          }
        }
      }
    }

    "work fine when using Future.collect" in {
      val fs: Future[Seq[String]] = Future.collect(Seq(f1, f2))
      fs foreach {
        seq: Seq[String] => seq foreach {
          v: String => v should (equal("Hello, World!") or equal("Hello, Twitter!"))
        }
      }
      for {
        seq: Seq[String] <- fs
        v: String <- seq
      } v should (equal("Hello, World!") or equal("Hello, Twitter!"))
    }

    "work fine when using Future.times" in {
      val iteration: Future[Unit] = Future.times(3)(f1)
      iteration foreach {
        _ => println("3 times iteration completed successfully!")
      }
      iteration onFailure {
        _ => fail("The iteration failed!")
      }
    }

    // http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html
    "work fine with toJavaFuture" in {
      type JavaFuture[T] = java.util.concurrent.Future[T]
      val sf: Future[_] = Future.value("foo")
      val jf: JavaFuture[_] = sf.toJavaFuture
      jf.get() should equal("foo")
    }

    "work fine with transform" in {
      // normal
      val f1 = Future.value("foo") transform {
        case Return(v) => Future.value(v * 2)
      }
      f1 onSuccess {
        v => v should equal("foofoo")
      } onFailure {
        t => fail("This should not be called.")
      }
      // exception
      val f2 = Future.exception(new RuntimeException) transform {
        case Throw(t) => Future.value("---")
      }
      f2 onSuccess {
        v => v should equal("---")
      } onFailure {
        t => fail("This should not be called.")
      }
    }

  }

}
