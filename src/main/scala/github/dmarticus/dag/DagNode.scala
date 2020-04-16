package github.dmarticus.dag

import github.dmarticus.dag.LazyNode._

import scala.concurrent.{Await, ExecutionContext, Future}

sealed trait DAGNode[K, V] {
  def name: K
}

object DAGNode {
  def toLazyNetWork[K, V](nodes: Seq[DAGNode[K, V]]): Map[K, LazyNode[V]] = {
    lazy val nodesMap: LazyNode[Map[K, LazyNode[V]]] =
      lazyNode(nodes.map(toLazyCell).toMap)

    def toLazyCell(node: DAGNode[K, V]): (K, LazyNode[V]) =
      node match {
        case InputNode(k, f) => k -> lazyNode(f())
        case InternalNode(k, ds, f) =>
          k -> nodesMap.flatMap { m =>
            val inputs = ds.map(d =>
              m.getOrElse(d, throw new NoSuchElementException(d.toString)))
            sequence(inputs).map(f)
          }
      }

    nodesMap.getValue()
  }

  def toFutureNetwork[K, V](nodes: Seq[DAGNode[K, V]])
                           (implicit executor: ExecutionContext): Map[K, LazyNode[Future[V]]] = {
    def toFutureNode(node: DAGNode[K, V]): DAGNode[K, Future[V]] = {
      node match {
        case InputNode(k, f) =>
          // TODO use Kleisli to compose f with future.apply
          val futureF = () => Future(f())(executor)
          InputNode(k, futureF)
        case InternalNode(k, deps, f) =>
          val futureF = (xs: Seq[Future[V]]) => Future.sequence(xs)(implicitly, executor).map(f)
          InternalNode(k, deps, futureF)
      }
    }

    toLazyNetWork(nodes.map(toFutureNode))
  }

  case class LazyFuture[A](l: LazyNode[Future[A]]) {

    import scala.concurrent.duration._

    val TIMEOUT_IN_SECONDS = 10

    def getFuture(timeOut: Int = TIMEOUT_IN_SECONDS): A =
      Await.result(l.getValue(), timeOut.seconds)
  }

}

case class InputNode[K, V](override val name: K, mapImpl: () => V)
  extends DAGNode[K, V]

private case class InternalNode[K, V](override val name: K,
                                      depends: Seq[K],
                                      reduceImpl: Seq[V] => V)
  extends DAGNode[K, V]

object ProcessNode {
  def apply[K, V](name: K,
                  depends: Seq[K],
                  reduceImpl: Seq[V] => V): DAGNode[K, V] =
    InternalNode(name, depends, reduceImpl)

  def apply[K, V](name: K, depend: K, mapImpl: V => V): DAGNode[K, V] =
    apply(name, Seq(depend), (ss: Seq[V]) => mapImpl(ss.head))
}

object OutputNode {
  def apply[K, V](name: K,
                  depends: Seq[K],
                  reduceImpl: Seq[V] => Unit): DAGNode[K, V] =
    InternalNode(name, depends, (ss: Seq[V]) => {
      reduceImpl(ss); ss.head
    })

  def apply[K, V](name: K, dependencies: K, mapImpl: V => Unit): DAGNode[K, V] =
    apply(name, Seq(dependencies), (ss: Seq[V]) => mapImpl(ss.head))
}
