package github.dmarticus.dag.parallel

import java.util.concurrent.Executors

import github.dmarticus.dag.common.TestHelpers
import github.dmarticus.dag.core.{InputNode, InternalNode, ProcessNode}
import github.dmarticus.dag.parallel.Implicits._
import org.scalatest.funspec.AnyFunSpec

import scala.concurrent.ExecutionContext

class DagTest extends AnyFunSpec with TestHelpers {
  describe("For lazy evaluation of a concurrent network") {
    describe("nodes are only evaluated once") {
      it("when fetched in sequence") {

        // build network
        val nodes = Seq(InputNode("input", () => System.currentTimeMillis()))
        val futureNetwork = Nodes(nodes).toLazyNetwork

        val before = futureNetwork("input").getValue
        Thread.sleep(1)
        val after = futureNetwork("input").getValue

        assert(before === after)
      }
    }

    it("when fetched in parallel") {

      // build network
      val nodes = Seq(
        InputNode("input", () => { Thread.sleep(1000); System.currentTimeMillis()}), // wait 1 second
        ProcessNode("fetch1", "input", (x: Long) => x),
        ProcessNode("fetch2", "input", (x: Long) => x),
        ProcessNode("fetch3", "input", (x: Long) => x),
        ProcessNode("merge", Seq("fetch1", "fetch2", "fetch3"),
                    (xs: Seq[Long]) => xs.toSet.size.toLong))

      val network = Nodes(nodes).toLazyNetwork
      assert(network("merge").getValue === 1)
    }

    it("when run in parallel") {
      import java.util.concurrent.Executors
      import scala.concurrent.ExecutionContext

      val executor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

      case class TimeRecord(id: String, start: Long, end: Long)

      // build network
      val input1 = InputNode("input1", parallelRead("input1", 0))
      val input2 = InputNode("input2", parallelRead("input2", 100))
      val input3 = InputNode("input3", parallelRead("input3", 50))
      val process1 = ProcessNode(
        "process1",
        Seq("input1", "input2", "input3"),
        parallelProcess("process1") _)
      val nodes = Seq(input1, input2, input3, process1)

      val fm = nodes.toLazyNetwork(executor)

      assert(
        fm("process1").getValue
          .sortBy(_.start)
          .sliding(2)
          .exists(x => x(1).start < x(0).end))
    }
  }
}
