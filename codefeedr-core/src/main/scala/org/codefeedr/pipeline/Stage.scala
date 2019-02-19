/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codefeedr.pipeline

import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.scala.DataStream
import org.codefeedr.Properties
import org.codefeedr.buffer.BufferFactory

import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe._

/** This class represents a stage within a pipeline. I.e. a node in the graph.
  *
  * @tparam In  Input type for this stage.
  * @tparam Out Output type for this stage.
  */
protected abstract class Stage[
    In <: Serializable with AnyRef: ClassTag: TypeTag,
    Out <: Serializable with AnyRef: ClassTag: TypeTag](
    val stageId: Option[String] = None) {

  /** The pipeline this stage belongs to. */
  var pipeline: Pipeline = _

  /** Get the StreamExecutionEnvironment. */
  def environment = pipeline.environment

  /** Get the id of this stage */
  def id: String = stageId.getOrElse(getClass.getName)

  /** Get the type of IN */
  def getInType = classTag[In].runtimeClass.asInstanceOf[Class[In]]

  /** Get the type of OUT */
  def getOutType = classTag[Out].runtimeClass.asInstanceOf[Class[Out]]

  /** Get the properties of this stage.
    *
    * @return The properties of this stage.
    */
  def properties: Properties =
    pipeline.propertiesOf(this)

  /** Setups the pipeline object with a pipeline.
    *
    * @param pipeline The pipeline it belongs to.
    */
  def setUp(pipeline: Pipeline): Unit = {
    this.pipeline = pipeline
  }

  /** Transforms the stage from its input type to its output type.
    * This requires using the Flink DataStream API.
    *
    * @param source The input source with type In.
    * @return The transformed stream with type Out.
    */
  def transform(source: DataStream[In]): DataStream[Out]

  /** Verify that the stage is valid.
    * Checks types of the input sources and whether the graph is configured correctly for the types.
    */
  protected[pipeline] def verifyGraph(graph: DirectedAcyclicGraph): Unit = {}

  /** Get all parents of this stage.
    *
    * @return The parents of this stage, can be empty.
    */
  def getParents
    : Vector[Stage[Serializable with AnyRef, Serializable with AnyRef]] =
    pipeline.graph
      .getParents(this)
      .asInstanceOf[Vector[
        Stage[Serializable with AnyRef, Serializable with AnyRef]]]

  /** Check if this stage is sourced from a [[org.codefeedr.buffer.Buffer]].
    *
    * @return True if this stage has a Buffer source.
    */
  def hasMainSource: Boolean =
    typeOf[In] != typeOf[Nothing] && pipeline.graph
      .getFirstParent(this)
      .isDefined

  /** Check if this stage is sinked to a [[org.codefeedr.buffer.Buffer]].
    *
    * @return True if this stage has a Buffer sink.
    */
  def hasSink: Boolean = typeOf[Out] != typeOf[Nothing]

  /** Returns the (main)buffer source of this stage.
    * The main source is the first parent of this stage. Other sources need to be joined in the Flink job.
    *
    * @return The DataStream resulting from the buffer.
    */
  def getMainSource(groupId: String = null): DataStream[In] = {
    assert(pipeline != null)

    if (!hasMainSource) {
      throw NoSourceException(
        "Stage defined NoType as In type. Buffer can't be created.")
    }

    // Get the first parent.
    val parentNode = getParents(0)

    // Create a buffer and return the source.
    val factory = new BufferFactory(pipeline, this, parentNode, groupId)
    val buffer = factory.create[In]()

    buffer.getSource
  }

  /** Returns the buffer sink of this stage.
    *
    * @return The SinkFunction resulting from the buffer.
    */
  def getSink(groupId: String = null): SinkFunction[Out] = {
    assert(pipeline != null)

    if (!hasSink) {
      throw NoSinkException(
        "PipelineObject defined NoType as Out type. Buffer can't be created.")
    }

    // Create a buffer and return the sink.
    val factory = new BufferFactory(pipeline, this, this, groupId)
    val buffer = factory.create[Out]()

    buffer.getSink
  }

  /** Get the sink subject used by the buffer.
    * This is also used for child objects to read from the buffer again.
    *
    * @return Sink subject which is basically the stage id.
    */
  def getId: String = this.id

  /** Returns the buffer source of this stage.
    *
    * @return The DataStream resulting from the buffer.
    */
  def getSource[T <: Serializable with AnyRef: ClassTag: TypeTag](
      parentNode: Stage[Serializable with AnyRef, Serializable with AnyRef])
    : DataStream[T] = {
    assert(parentNode != null)

    val factory = new BufferFactory(pipeline, this, parentNode)
    val buffer = factory.create[T]()

    buffer.getSource
  }

  /** Create a list of stages by appending another stage.
    *
    * @param stage The other stage to add.
    * @return A new StageList with the stage added.
    */
  def :+[U <: Serializable with AnyRef, V <: Serializable with AnyRef](
      stage: Stage[U, V]): StageList =
    inList.add(stage)

  /** Create a [[StageList]] with this stage.
    *
    * @return [[StageList]] containing this stage.
    */
  def inList: StageList =
    new StageList().add(this)
}
