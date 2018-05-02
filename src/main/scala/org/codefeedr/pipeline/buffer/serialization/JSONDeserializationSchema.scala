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
package org.codefeedr.pipeline.buffer.serialization

import org.apache.flink.api.common.serialization.AbstractDeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization

import scala.reflect.{Manifest, classTag}

class JSONDeserializationSchema[T <: AnyRef : Manifest] extends AbstractDeserializationSchema[T] {

  //get type of class
  val inputClassType : Class[T] = classTag[T].runtimeClass.asInstanceOf[Class[T]]
  /**
    * Deserializes a (JSON) message into a (generic) case class
    * @param message the message to deserialized.
    * @return a deserialized case class.
    */
  override def deserialize(message: Array[Byte]): T = {
    implicit val formats = Serialization.formats(NoTypeHints)
    Serialization.read[T](new String(message))
  }

  override def getProducedType: TypeInformation[T] = {
    TypeExtractor.createTypeInfo(inputClassType)
  }
}
