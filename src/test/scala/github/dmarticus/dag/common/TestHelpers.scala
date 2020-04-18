package github.dmarticus.dag.common

import java.util.concurrent.ConcurrentHashMap

case class ExecutionTimestamp(id: String, start: Long, end: Long)

trait TestHelpers {

  val TIMEOUT_IN_MS = 10
  val RUNTIME_IN_MS = 1000

  def parallelRead(
      id: String,
      delayInMillis: Int = TIMEOUT_IN_MS,
      runTimeInMillis: Int = RUNTIME_IN_MS)(): Seq[ExecutionTimestamp] = {
    Thread.sleep(delayInMillis)
    val start = System.currentTimeMillis()
    Thread.sleep(runTimeInMillis)
    val end = System.currentTimeMillis()

    List(ExecutionTimestamp(id, start, end))
  }

  def parallelProcess(id: String)(
      st: Seq[Seq[ExecutionTimestamp]]): Seq[ExecutionTimestamp] =
    parallelRead(id)() ++ st.reduce(_ ++ _)

  val records = new ConcurrentHashMap[String, Int]()

  def incr(id: String): Unit = {
    val t = records.getOrDefault(id, 0) + 1
    records.put(id, t)
  }

  // input function: () => A
  def read(name: String, value: Int)(): Int = {
    incr(name)
    value
  }

  // process function: A => A
  def processOneDepend(name: String)(s: Int): Int = {
    incr(name)
    s * 2
  }

  // process function: Seq[A] => A
  def processMultiDepends(name: String)(ss: Seq[Int]): Int = {
    incr(name)
    ss.sum
  }

  // output function: Seq[A] => ()
  def write(name: String)(ss: Seq[Int]): Unit = {
    incr(name)
  }

}
