package org.codefeedr.pipeline

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.codefeedr.ImmutableProperties
import org.codefeedr.pipeline.buffer.BufferType.BufferType

case class Pipeline(bufferType: BufferType,
                    bufferProperties: ImmutableProperties,
                    objects: Seq[PipelineObject[PipelinedItem, PipelinedItem]],
                    properties: ImmutableProperties) {

  def getEnvironment : StreamExecutionEnvironment = {
    StreamExecutionEnvironment.getExecutionEnvironment
  }


  def start(args: Array[String]): Unit = {
    start(1)
  }

  def start(options: Int): Unit = {
    // TODO: decide which to start
    startMock(options)
  }

  // Without any buffers. Connect all POs to each other
  def startMock(options: Int): Unit = {
    val env = getEnvironment

    // Run all setups
    for (obj <- objects) {
      obj.setUp(this)
    }

    // Connect each object by getting a starting buffer, if any, and sending it to the next.
    var buffer: DataStream[PipelinedItem] = null
    for (obj <- objects) {
      buffer = obj.transform(buffer)
    }

    env.execute("CodeFeedr Mock Job")
  }

  // With buffers, all in same program
  def startLocal(options: Int): Unit = {
    val env = getEnvironment

    // Run all setups
    for (obj <- objects) {
      obj.setUp(this)
    }

    // For each PO, make buffers and run
    for (obj <- objects) {
      runObject(obj)
    }

    env.execute("CodeFeedr Local Job")
  }

  // With buffers, running just one PO
  def startClustered(options: Int): Unit = {
    val env = getEnvironment

    // TODO: find that PO
    val obj = objects.head
    obj.setUp(this)
    runObject(obj)

    env.execute("CodeFeedr Cluster Job")
  }

  private def runObject(obj: PipelineObject[PipelinedItem, PipelinedItem]): Unit = {
    val source = obj.getSource
    val sink = obj.getSink

    obj
      .transform(source)
      .addSink(sink)
  }

}