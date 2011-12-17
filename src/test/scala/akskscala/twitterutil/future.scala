package akskscala.twitterutil

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import com.twitter.util.{Try, Future}

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


    "work fine when using Future.join" in {
      // join
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

  }

}