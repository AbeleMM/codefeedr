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
package org.codefeedr.plugins.github.stages

import java.util

import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.scala.DataStream
import org.codefeedr.pipeline.PipelineBuilder
import org.codefeedr.plugins.github.GitHubProtocol.{IssueCommentEvent, IssuesEvent}
import org.scalatest.FunSuite

class GitHubEventsToIssueCommentEventTest extends FunSuite {

  test ("GitHubEventsToIssueCommentEvent integration test") {
    val pipeLine = new PipelineBuilder()
      .append(new SimpleEventSource("/issuecomment_events.json"))
      .append(new GitHubEventToIssueCommentEvent())
      .append { x : DataStream[IssueCommentEvent] =>
        x.addSink(new IssueCommentEventCollectSink)
      }
      .build()
      .startMock()

    //+- there are 2 events
    assert(IssueCommentEventCollectSink.result.size == 2)
  }

}

object IssueCommentEventCollectSink {
  val result = new util.ArrayList[IssueCommentEvent]() //mutable list
}

class IssueCommentEventCollectSink extends SinkFunction[IssueCommentEvent] {

  override def invoke(value: IssueCommentEvent): Unit = {
    synchronized {
      IssueCommentEventCollectSink.result.add(value)
    }
  }

}
