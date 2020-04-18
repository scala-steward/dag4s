package github.dmarticus.dag.serial

import github.dmarticus.dag.Result
import github.dmarticus.dag.core.{DAGNode, InputNode, InternalNode, LazyNode}
import github.dmarticus.dag.core.LazyNode._

import scala.concurrent.duration.Duration

object Implicits { self =>
  private[dag] def toLazyNetwork[K, V](
      nodes: Seq[DAGNode[K, V]]): Map[K, LazyNode[V]] = {
    lazy val nodesMap: LazyNode[Map[K, LazyNode[V]]] =
      lazyNode(nodes.map(toLazyNode).toMap)

    def toLazyNode(n: DAGNode[K, V]): (K, LazyNode[V]) =
      n match {
        case InputNode(k, f) => k -> lazyNode(f())
        case InternalNode(k, ds, f) =>
          k -> nodesMap.flatMap { m =>
            val inputs = ds.map(d =>
              m.getOrElse(d, throw new NoSuchElementException(d.toString)))
            sequence(inputs).map(f)
          }
      }

    nodesMap.get()
  }

  import scala.language.implicitConversions

  implicit class Nodes[K, V](nodes: Seq[DAGNode[K, V]]) {
    def toLazyNetwork: Map[K, LazyNode[V]] = self.toLazyNetwork(nodes)
  }

  implicit class SerResult[A](lc: LazyNode[A]) extends Result[A] {
    def getValue: A = lc.get()

    @deprecated("Invalid argument, use `getValue` instead.")
    def getValue(duration: Duration): A = getValue
  }
}
