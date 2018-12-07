/*
 * Copyright 2017 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow.rx2

import com.squareup.workflow.WorkflowPool
import com.squareup.workflow.makeWorkflowId
import io.reactivex.subjects.SingleSubject
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import java.io.IOException

class WorkerIntegrationTest {

  private val pool = WorkflowPool()
  private val single = SingleSubject.create<Unit>()
  private val worker = single.asWorker()

  @Test fun `when call succeeds`() {
    val reaction = pool.workerResult(worker, Unit)
        .test()
    reaction.assertNotTerminated()

    single.onSuccess(Unit)

    reaction.assertValue(Unit)
  }

  @Test fun `when call fails`() {
    val reaction = pool.workerResult(worker, Unit)
        .test()
    reaction.assertNotTerminated()

    single.onError(IOException("network fail"))

    val failure = reaction.errors()
        .single()
    assertThat(failure).isInstanceOf(IOException::class.java)
  }

  @Test fun `when workflow cancelled`() {
    val reaction = pool.workerResult(worker, Unit)
        .test()
    reaction.assertNotTerminated()

    pool.abandonDelegate(worker.makeWorkflowId())

    // The rx2 version of nextProcessResult will never complete the single if the workflow is
    // cancelled.
    reaction.assertNoValues()
    reaction.assertNoErrors()
  }
}
