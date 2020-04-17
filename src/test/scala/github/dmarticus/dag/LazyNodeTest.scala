package github.dmarticus.dag

import java.util.concurrent.ConcurrentHashMap

import github.dmarticus.dag.common.TestHelpers
import org.scalatest.funspec.AnyFunSpec

class LazyNodeTest extends AnyFunSpec with TestHelpers {
  describe("For any node of the directed network,") {
    it("is lazy and evaluated only once.") {
      /* ------- records for method execute times --------- */
      val records = new ConcurrentHashMap[String, Int]()

      def incr(id: String): Unit = {
        val t = records.getOrDefault(id, 0) + 1
        records.put(id, t)
      }

      /* ------- helper method --------- */
      /* input function: () => A */
      def read(name: String, value: Int)(): Int = {
        incr(name)
        value
      }

      /* process function: A => A */
      def processOneDepend(name: String)(s: Int): Int = {
        incr(name)
        s * 2
      }

      /* process function: Seq[A] => A */
      def processMultiDepends(name: String)(ss: Seq[Int]): Int = {
        incr(name)
        ss.sum
      }

      /* output function: Seq[A] => () */
      def write(name: String)(ss: Seq[Int]): Unit = {
        incr(name)
      }

      /* ------- build network --------- */
      val input1 = InputNode("i1", read("i1", 1))
      val input2 = InputNode("i2", read("i2", 2))
      val input3 = InputNode("i3", read("i3", 3))
      val process1 = ProcessNode("p1", "i1", processOneDepend("p1") _)
      val process2 =
        ProcessNode("p2", Seq("i2", "i3"), processMultiDepends("p2") _)
      val output1 = OutputNode("o1", Seq("p2", "i2"), write("o1") _)
      val output2 = OutputNode("o2", Seq("p2", "p1"), write("o2") _)

      val nodes =
        Seq(input1, input2, input3, process1, process2, output1, output2)

      val network: Map[String, LazyNode[Int]] = DAGNode.toLazyNetWork(nodes)
      assert(network.size === 7)

      network("o1").getValue()
      assert(records.values().toArray().forall(_ === 1))
      assert(records.size === 4)
      assert(records.containsKey("p2"))
      assert(records.containsKey("o1"))
      assert(records.containsKey("i2"))
      assert(records.containsKey("i3"))

      network("o2").getValue()
      assert(records.values().toArray().forall(_ === 1))
      assert(records.size === network.size)

      assert(network("p1").getValue() === 2)
      assert(network("p2").getValue() === 5)
      assert(records.values().toArray().forall(_ === 1))
    }
  }
  describe("For lazy evaluation of a concurrent network") {
    it("nodes are only evaluated once") {
      val nodes = Seq(InputNode("input", () => System.currentTimeMillis()))
      val futureNetwork = DAGNode.toFutureNetwork(nodes)

      val before = futureNetwork("input").getFuture()
      Thread.sleep(1)
      val after = futureNetwork("input").getFuture()

      assert(before === after)
    }

    it("nodes are executed in parallel") {
      // build network
      val input1 = InputNode("input1", read("input1", 0))
      val input2 = InputNode("input2", read("input2", 100))
      val input3 = InputNode("input3", read("input3", 50))
      val process1 = InternalNode(
        "process1",
        Seq("input1", "input2", "input3"),
        process("process1"))

      val nodes = Seq(input1, input2, input3, process1)
      val futureNetwork = DAGNode.toFutureNetwork(nodes)

      assert(
        futureNetwork("process1").getFuture()
          .sortBy(_.start)
          .sliding(2)
          .exists(x => x(1).start < x(0).end))
    }
  }

  }
