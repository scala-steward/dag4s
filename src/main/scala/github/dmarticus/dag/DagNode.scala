package github.dmarticus.dag

import LazyNode._

sealed trait DAGNode[K, V] {
  def name: K
}

object DAGNode {
  def toLazyNetWork[K, V](nodes: Seq[DAGNode[K, V]]): Map[K, LazyNode[V]] = {
    lazy val nodesMap: LazyNode[Map[K, LazyNode[V]]] =
      lazyNode(nodes.map(toLazyCell).toMap)

    def toLazyCell(n: DAGNode[K, V]): (K, LazyNode[V]) =
      n match {
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
}

case class InputNode[K, V](override val name: K, transFunc: () => V)
    extends DAGNode[K, V]

private case class InternalNode[K, V](override val name: K,
                                      depends: Seq[K],
                                      reduceFunc: Seq[V] => V)
    extends DAGNode[K, V]

object ProcessNode {
  def apply[K, V](name: K,
                  depends: Seq[K],
                  reduceFunc: Seq[V] => V): DAGNode[K, V] =
    InternalNode(name, depends, reduceFunc)

  def apply[K, V](name: K, depend: K, transFunc: V => V): DAGNode[K, V] =
    apply(name, Seq(depend), (ss: Seq[V]) => transFunc(ss.head))
}

object OutputNode {
  def apply[K, V](name: K,
                  depends: Seq[K],
                  reduceFunc: Seq[V] => Unit): DAGNode[K, V] =
    InternalNode(name, depends, (ss: Seq[V]) => { reduceFunc(ss); ss.head })

  def apply[K, V](name: K, depend: K, transFunc: V => Unit): DAGNode[K, V] =
    apply(name, Seq(depend), (ss: Seq[V]) => transFunc(ss.head))
}
