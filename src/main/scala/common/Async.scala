package common

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object Async {

  def task[T](task: T)(implicit ec: ExecutionContext) =
    Future(task)

  def waitForCompletion[T](aSingleFuture: Future[Iterator[T]]): Iterator[T] = {
    Await.result(aSingleFuture, 5.minutes)
  }

}
