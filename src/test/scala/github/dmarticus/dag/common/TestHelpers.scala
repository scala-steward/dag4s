package github.dmarticus.dag.common

case class ExecutionTimestamp(id: String, start: Long, end: Long)

trait TestHelpers {

  val TIMEOUT_IN_MS = 10
  val RUNTIME_IN_MS = 1000

  def read(id: String, delayInMillis: Int = TIMEOUT_IN_MS, runTimeInMillis: Int = RUNTIME_IN_MS)(): Seq[ExecutionTimestamp] = {
    Thread.sleep(delayInMillis)
    val start = System.currentTimeMillis()
    Thread.sleep(runTimeInMillis)
    val end = System.currentTimeMillis()

    List(ExecutionTimestamp(id, start, end))
  }

  def process(id: String)(st: Seq[Seq[ExecutionTimestamp]]): Seq[ExecutionTimestamp] =
    read(id)() ++ st.reduce(_ ++ _)

}
