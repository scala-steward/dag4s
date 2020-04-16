package github.dmarticus.dag

import github.dmarticus.dag.DAGNode.{LazyFuture, LazyNodeOps}

import scala.concurrent.Future

case class LazyNode[+A](get: () => A) {

  import LazyNode._

  def map[B](f: A => B): LazyNode[B] = lazyNode(f(get()))

  def flatMap[B](f: A => LazyNode[B]): LazyNode[B] = map(f(_).get())

  def map2[B, C](that: LazyNode[B])(f: (A, B) => C): LazyNode[C] =
    for {
      a <- this
      b <- that
    } yield f(a, b)
}

object LazyNode {
  def lazyNode[A](f: => A): LazyNode[A] = {
    lazy val value = f
    LazyNode(() => value)
  }

  def sequence[A](as: Seq[LazyNode[A]]): LazyNode[Seq[A]] =
    lazyNode(as.map(_.get()))

  implicit def asLazyFuture[A](ln: LazyNode[Future[A]]): LazyFuture[A] = LazyFuture(ln)

  implicit def asLazyNodeOps[A](l: LazyNode[A]): LazyNodeOps[A] = LazyNodeOps(l)
}
