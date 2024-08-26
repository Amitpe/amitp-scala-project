package common

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Async {

  def waitForCompletion[T](aSingleFuture: Future[Iterator[T]]): Iterator[T] = {
    Await.result(aSingleFuture, 5.minutes)
  }

}
