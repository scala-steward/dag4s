package github.dmarticus.dag.parallel

import github.dmarticus.dag.Result
import github.dmarticus.dag.core.{DAGNode, InputNode, InternalNode, LazyNode}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object Implicits { self =>
  private[dag] def toParallel[K, V](nodes: Seq[DAGNode[K, V]])
                                   (implicit executor: ExecutionContext
                                   ): Seq[DAGNode[K, Future[V]]] = {

    def toFutureCell(n: DAGNode[K, V]): DAGNode[K, Future[V]] =
      n match {
        case InputNode(k, f) =>
          // TODO use Kleisli types to compose `f` and `Future.apply`
          val g = () => Future(f())
          InputNode(k, g)
        case InternalNode(k, ds, f) =>
          val g = (xs: Seq[Future[V]]) => Future.sequence(xs).map(f)
          InternalNode(k, ds, g)
      }

    nodes.map(toFutureCell)
  }

  private[dag] def toLazyNetwork[K, V](nodes: Seq[DAGNode[K, V]])
                                      (executor: ExecutionContext): Map[K, LazyNode[Future[V]]] = {
    import github.dmarticus.dag.serial

    serial.Implicits.toLazyNetwork(self.toParallel(nodes)(executor))
  }


  import scala.language.implicitConversions

  implicit class Nodes[K, V](nodes: Seq[DAGNode[K, V]]) {
    import scala.concurrent.ExecutionContext.Implicits.global

    def toLazyNetwork: Map[K, LazyNode[Future[V]]] = this.toLazyNetwork(global)

    def toLazyNetwork(executor: ExecutionContext): Map[K, LazyNode[Future[V]]] =
      self.toLazyNetwork(nodes)(executor)

  }

  implicit class ParResult[A](lc: LazyNode[Future[A]]) extends Result[A] {
    def getValue: A = getValue(Duration.Inf)

    def getValue(duration: Duration): A =
      Await.result(lc.get(), duration)
  }
}
