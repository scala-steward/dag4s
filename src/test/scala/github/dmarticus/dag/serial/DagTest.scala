package github.dmarticus.dag.serial

import java.util.concurrent.ConcurrentHashMap

import github.dmarticus.dag.common.TestHelpers
import github.dmarticus.dag.core.{
  InputNode,
  InternalNode,
  LazyNode,
  OutputNode,
  ProcessNode
}
import github.dmarticus.dag.serial.Implicits._
import org.scalatest.funspec.AnyFunSpec

class DagTest extends AnyFunSpec with TestHelpers {
  describe("For any node of the directed network,") {
    it("is lazy and evaluated only once.") {
      // build network
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

      val network: Map[String, LazyNode[Int]] = Nodes(nodes).toLazyNetwork
      assert(network.size === 7)

      network("o1").get()
      assert(records.values().toArray().forall(_ === 1))
      assert(records.size === 4)
      assert(records.containsKey("p2"))
      assert(records.containsKey("o1"))
      assert(records.containsKey("i2"))
      assert(records.containsKey("i3"))

      network("o2").get()
      assert(records.values().toArray().forall(_ === 1))
      assert(records.size === network.size)

      assert(network("p1").get() === 2)
      assert(network("p2").get() === 5)
      assert(records.values().toArray().forall(_ === 1))
    }
  }
}
