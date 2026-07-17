package org.projectcontinuum.core.orchestration.workflow

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.temporal.api.enums.v1.IndexedValueType
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.client.WorkflowFailedException
import io.temporal.client.WorkflowOptions
import io.temporal.client.WorkflowStub
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.failure.ApplicationFailure
import io.temporal.failure.CanceledFailure
import io.temporal.testing.TestEnvironmentOptions
import io.temporal.testing.TestWorkflowEnvironment
import org.junit.jupiter.api.Test
import org.projectcontinuum.core.commons.activity.IContinuumNodeActivity
import org.projectcontinuum.core.commons.constant.TaskQueues
import org.projectcontinuum.core.commons.model.ContinuumWorkflowModel
import org.projectcontinuum.core.commons.model.PortData
import org.projectcontinuum.core.commons.model.PortDataStatus
import org.projectcontinuum.core.commons.protocol.progress.ContinuumNodeActivitySignal
import org.projectcontinuum.core.commons.protocol.progress.NodeProgress
import org.projectcontinuum.core.commons.workflow.IContinuumWorkflow
import org.projectcontinuum.core.orchestration.workflow.fixtures.FakeContinuumNodeActivity
import org.projectcontinuum.core.orchestration.workflow.fixtures.FakeInitializeActivity
import org.projectcontinuum.core.orchestration.workflow.fixtures.WorkflowFixtures
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure Temporal unit tests for [ContinuumWorkflow] using [TestWorkflowEnvironment] — no Spring
 * context, no Kafka. Workflow definitions are loaded from classpath `.cwf.json` resources via
 * [WorkflowFixtures]; node execution is driven by [FakeContinuumNodeActivity] /
 * [FakeInitializeActivity] instead of real node business logic.
 */
class ContinuumWorkflowTest {

  private val activityTaskQueue = "TEST_FAKE_CONTINUUM_NODE_ACTIVITY_TASK_QUEUE"

  private class Env(
    val testEnv: TestWorkflowEnvironment,
    val client: WorkflowClient,
    val fakeNodeActivity: FakeContinuumNodeActivity
  ) : Closeable {
    override fun close() = testEnv.close()
  }

  private fun newEnv(dropOneNode: Boolean = false): Env {
    val mapper = JacksonJsonPayloadConverter.newDefaultObjectMapper()
      .apply { registerModule(KotlinModule.Builder().build()) }
    val dataConverter = DefaultDataConverter.newDefaultInstance()
      .withPayloadConverterOverrides(JacksonJsonPayloadConverter(mapper))

    val testEnv = TestWorkflowEnvironment.newInstance(
      TestEnvironmentOptions.newBuilder()
        .setWorkflowClientOptions(WorkflowClientOptions.newBuilder().setDataConverter(dataConverter).build())
        .registerSearchAttribute("Continuum:ExecutionStatus", IndexedValueType.INDEXED_VALUE_TYPE_INT)
        .registerSearchAttribute("Continuum:WorkflowFileName", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD)
        .build()
    )

    testEnv.newWorker(TaskQueues.WORKFLOW_TASK_QUEUE)
      .registerWorkflowImplementationTypes(ContinuumWorkflow::class.java)

    val fakeNodeActivity = FakeContinuumNodeActivity()
    testEnv.newWorker(activityTaskQueue).registerActivitiesImplementations(fakeNodeActivity)

    testEnv.newWorker(TaskQueues.ACTIVITY_TASK_QUEUE_INITIALIZE)
      .registerActivitiesImplementations(FakeInitializeActivity(activityTaskQueue, dropOneNode))

    testEnv.start()
    return Env(testEnv, testEnv.workflowClient, fakeNodeActivity)
  }

  private fun startWorkflow(
    env: Env,
    model: ContinuumWorkflowModel,
    workflowId: String = "test-${UUID.randomUUID()}"
  ): Pair<IContinuumWorkflow, CompletableFuture<Map<String, Map<String, PortData>>>> {
    val stub = env.client.newWorkflowStub(
      IContinuumWorkflow::class.java,
      WorkflowOptions.newBuilder()
        .setWorkflowId(workflowId)
        .setTaskQueue(TaskQueues.WORKFLOW_TASK_QUEUE)
        .build()
    )
    val future = WorkflowClient.execute(stub::start, model)
    return stub to future
  }

  private fun samplePortData(value: String = "sample"): PortData =
    PortData(status = PortDataStatus.SUCCESS, contentType = "text/plain", tableSpec = emptyList(), data = value)

  private fun awaitInvoked(fake: FakeContinuumNodeActivity, nodeId: String, timeoutMs: Long = 5000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!fake.invokedNodeIds.contains(nodeId)) {
      if (System.currentTimeMillis() > deadline) error("Timed out waiting for node $nodeId to be invoked")
      Thread.sleep(10)
    }
  }

  @Test
  fun `happy path linear workflow completes and returns all node outputs`() {
    newEnv().use { env ->
      env.fakeNodeActivity.successOutputs["2"] =
        mapOf("output-1" to samplePortData("time"))
      env.fakeNodeActivity.successOutputs["3"] =
        mapOf("output-1" to samplePortData("part1"), "output-2" to samplePortData("part2"))

      val model = WorkflowFixtures.loadWorkflow("/test-workflows/linear-2node.cwf.json")
      val (_, future) = startWorkflow(env, model)
      val result = future.get(10, TimeUnit.SECONDS)

      assertTrue(result.containsKey("2"))
      assertTrue(result.containsKey("3"))
    }
  }

  @Test
  fun `diamond DAG executes fan-out nodes and only runs fan-in node after both complete`() {
    newEnv().use { env ->
      listOf("A", "B", "C", "D").forEach { id ->
        env.fakeNodeActivity.successOutputs[id] = mapOf("out-1" to samplePortData(id))
      }

      val model = WorkflowFixtures.loadWorkflow("/test-workflows/diamond-4node.cwf.json")
      val (_, future) = startWorkflow(env, model)
      val result = future.get(10, TimeUnit.SECONDS)

      assertEquals(setOf("A", "B", "C", "D"), result.keys)

      val invoked = env.fakeNodeActivity.invokedNodeIds
      val dIndex = invoked.indexOf("D")
      assertTrue(dIndex > invoked.indexOf("B"))
      assertTrue(dIndex > invoked.indexOf("C"))
    }
  }

  @Test
  fun `node returning dollar-error output fails workflow with ApplicationFailure and preserves prior successes`() {
    newEnv().use { env ->
      env.fakeNodeActivity.successOutputs["2"] =
        mapOf("output-1" to samplePortData("time"))
      env.fakeNodeActivity.errorOutputs["3"] =
        mapOf(IContinuumNodeActivity.NodeOutputSystemPort.ERROR.key to samplePortData("boom"))

      val model = WorkflowFixtures.loadWorkflow("/test-workflows/linear-2node.cwf.json")
      val (_, future) = startWorkflow(env, model)

      val executionException = assertFailsWith<ExecutionException> { future.get(10, TimeUnit.SECONDS) }
      val workflowFailed = executionException.cause as WorkflowFailedException
      val appFailure = workflowFailed.cause as ApplicationFailure
      assertEquals("WorkflowExecutionFailed", appFailure.type)

      @Suppress("UNCHECKED_CAST")
      val details = appFailure.details.get(0, Map::class.java) as Map<String, Map<*, *>>
      assertTrue(details.getValue("nodeErrors").containsKey("3"))
      assertTrue(details.getValue("nodeToOutputsMap").containsKey("2"))
    }
  }

  @Test
  fun `partial task queue resolution fails workflow before any node activity executes`() {
    newEnv(dropOneNode = true).use { env ->
      val model = WorkflowFixtures.loadWorkflow("/test-workflows/linear-2node.cwf.json")
      val (_, future) = startWorkflow(env, model)

      val executionException = assertFailsWith<ExecutionException> { future.get(10, TimeUnit.SECONDS) }
      val appFailure = (executionException.cause as WorkflowFailedException).cause as ApplicationFailure
      assertEquals("InitializationFailed", appFailure.type)
      assertTrue(env.fakeNodeActivity.invokedNodeIds.isEmpty())
    }
  }

  @Test
  fun `getWorkflowSnapshot query returns current outputs mid-execution and after completion`() {
    newEnv().use { env ->
      env.fakeNodeActivity.successOutputs["2"] =
        mapOf("output-1" to samplePortData("time"))
      env.fakeNodeActivity.successOutputs["3"] =
        mapOf("output-1" to samplePortData("part1"))
      val latch = CountDownLatch(1)
      env.fakeNodeActivity.blockUntil["3"] = latch

      val model = WorkflowFixtures.loadWorkflow("/test-workflows/linear-2node.cwf.json")
      val (stub, future) = startWorkflow(env, model)

      awaitInvoked(env.fakeNodeActivity, "3")
      val midSnapshot = stub.getWorkflowSnapshot()
      assertTrue(midSnapshot.nodeToOutputsMap.containsKey("2"))
      assertFalse(midSnapshot.nodeToOutputsMap.containsKey("3"))

      latch.countDown()
      future.get(10, TimeUnit.SECONDS)

      val finalSnapshot = stub.getWorkflowSnapshot()
      assertTrue(finalSnapshot.nodeToOutputsMap.containsKey("2"))
      assertTrue(finalSnapshot.nodeToOutputsMap.containsKey("3"))
    }
  }

  @Test
  fun `updateNodeProgressSignal updates progress without disrupting execution`() {
    newEnv().use { env ->
      env.fakeNodeActivity.successOutputs["2"] =
        mapOf("output-1" to samplePortData("time"))
      env.fakeNodeActivity.successOutputs["3"] =
        mapOf("output-1" to samplePortData("part1"))
      val latch = CountDownLatch(1)
      env.fakeNodeActivity.blockUntil["3"] = latch

      val model = WorkflowFixtures.loadWorkflow("/test-workflows/linear-2node.cwf.json")
      val (stub, future) = startWorkflow(env, model)

      awaitInvoked(env.fakeNodeActivity, "3")
      stub.updateNodeProgressSignal(
        ContinuumNodeActivitySignal(
          nodeId = "3",
          nodeProgress = NodeProgress(progressPercentage = 42, message = "halfway")
        )
      )

      latch.countDown()
      val result = future.get(10, TimeUnit.SECONDS)
      assertTrue(result.containsKey("3"))
    }
  }

  @Test
  fun `updateNodeProgressSignal with unknown node id fails the workflow task, not the workflow execution`() {
    newEnv().use { env ->
      env.fakeNodeActivity.successOutputs["2"] =
        mapOf("output-1" to samplePortData("time"))
      val latch = CountDownLatch(1)
      env.fakeNodeActivity.blockUntil["3"] = latch

      val model = WorkflowFixtures.loadWorkflow("/test-workflows/linear-2node.cwf.json")
      val workflowId = "test-unknown-node-signal-${UUID.randomUUID()}"
      val (stub, _) = startWorkflow(env, model, workflowId)

      awaitInvoked(env.fakeNodeActivity, "3")
      stub.updateNodeProgressSignal(
        ContinuumNodeActivitySignal(
          nodeId = "does-not-exist",
          nodeProgress = NodeProgress(progressPercentage = 0)
        )
      )

      // The internal NPE on an unknown node id fails the *workflow task*, not the workflow
      // execution — it never surfaces via the result future, only in execution history.
      val deadline = System.currentTimeMillis() + 5000
      var hasTaskFailure = false
      while (System.currentTimeMillis() < deadline && !hasTaskFailure) {
        val history = env.client.fetchHistory(workflowId)
        hasTaskFailure = history.events.any { it.hasWorkflowTaskFailedEventAttributes() }
        if (!hasTaskFailure) Thread.sleep(50)
      }
      assertTrue(hasTaskFailure, "Expected a WorkflowTaskFailed event from the unguarded NPE")

      latch.countDown()
      WorkflowStub.fromTyped(stub).terminate("test cleanup")
    }
  }

  @Test
  fun `node with unconnected declared input port is skipped while independent siblings still complete`() {
    newEnv().use { env ->
      env.fakeNodeActivity.successOutputs["root"] = mapOf("out-1" to samplePortData("root"))
      env.fakeNodeActivity.successOutputs["independent"] = mapOf("out-1" to samplePortData("independent"))

      val model = WorkflowFixtures.loadWorkflow("/test-workflows/skipped-input-3node.cwf.json")
      val (_, future) = startWorkflow(env, model)
      val result = future.get(10, TimeUnit.SECONDS)

      assertEquals(setOf("root", "independent"), result.keys)
      assertFalse(env.fakeNodeActivity.invokedNodeIds.contains("skip-me"))
    }
  }

  @Test
  fun `cancellation while a node is busy marks the workflow cancelled and rethrows to the client`() {
    newEnv().use { env ->
      env.fakeNodeActivity.successOutputs["2"] =
        mapOf("output-1" to samplePortData("time"))
      env.fakeNodeActivity.blockUntil["3"] = CountDownLatch(1) // never released

      val model = WorkflowFixtures.loadWorkflow("/test-workflows/linear-2node.cwf.json")
      val (stub, future) = startWorkflow(env, model)

      awaitInvoked(env.fakeNodeActivity, "3")
      WorkflowStub.fromTyped(stub).cancel()

      val executionException = assertFailsWith<ExecutionException> { future.get(10, TimeUnit.SECONDS) }
      val workflowFailed = executionException.cause as WorkflowFailedException
      assertTrue(workflowFailed.cause is CanceledFailure)
    }
  }
}
