package github.dmarticus.dag

import scala.concurrent.duration.Duration

trait Result[A] {
  def getValue: A

  def getValue(duration: Duration): A
}
