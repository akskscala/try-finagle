package akskscala.twitterutil;

import com.twitter.util.Future;
import scala.Function1;
import scala.runtime.AbstractFunction1;

import java.util.Arrays;
import java.util.List;

public class UsingFuture {

    public static void main(String[] args) throws Throwable {

        Function1<String, String> sayHello = new AbstractFunction1<String, String>() {
            public String apply(String input) {
                return "Hello, " + input + "!";
            }
        };

        // ----------------------------------------------
        // ///// Compile error /////
        // Function1<String, Unit> successCallback = new AbstractFunction1<String, Unit>() {
        //    public Unit apply(String input) {
        //        System.out.println("success!: " + input);
        //        return null;
        //    }
        // };
        // Future<String> f1 = Future.value(sayHello.apply("World")).onSuccess(successCallback);

        Future<String> f1 = Future.value(sayHello.apply("World"));
        Future<String> f2 = Future.value(sayHello.apply("Twitter"));
        Future<String> f3 = Future.value(sayHello.apply("Scala"));

        Future<List<String>> fs = Future.collect(Arrays.asList(f1, f2, f3));

        List<String> messages = (List<String>) fs.get();
        for (String message : messages) {
            System.out.println(message);
        }

    }

}