# dag4s

An experimental DAG library with support for FP scala and its associated FP libraries.

## Usage Example

1. Lazy Evaluation 
```scala
  // establish operators
  def read[A](): A
  def process1[A](a: A): A
  def process2[A](as: Seq[A]): A
  def write1[A](a: A): Unit
  def write2[A](as: Seq[A]): Unit
  
  // create nodes
  // InputNode(uid, function)
  val inputNode1 = InputNode("input1", read)
  val inputNode2 = InputNode("input2", read)
  
  // ProcessNode(uid, dependencyUids, function)
  val internalNode1 = ProcessNode("process1", "input1", process1)
  val internalNode2 = ProcessNode("process2", Seq("input1", "input2") process2)
  
  // OutputNode(uid, dependencyUids, function)
  val outputNode1 = OutputNode("output1", "process1", write1)
  val outputNode2 = OutputNode("output2", Seq("process1", "process2"), write2)
  val nodes = Seq(inputNode, internalNode1, internalNode2, outputNode1, outputNode2)
  
  // build network
  val network = DAGNode.toLazyNetWork(nodes)
  /**
   * input1 ---> process1 ---> output1
   *         |             |
   *         v             v
   * input2 ---> process2 ---> output2
   */

  // run if needed
  network("output1").getValue()
  network("output2").getValue()
```

2. Parallel Execution

```scala
 // build network
 val futureNetwork = DAGNode.toFutureNetWork(nodes)
 /**
  * input1 ---> process1 ---> output1
  *         |             |
  *         v             v
  * input2 ---> process2 ---> output2
  */
 
 // run if needed
 futureNetwork("output1").getFuture()
 futureNetwork("output2").getFuture()
```