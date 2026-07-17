package org.projectcontinuum.core.orchestration.workflow.fixtures

import org.projectcontinuum.core.commons.activity.IContinuumNodeActivity
import org.projectcontinuum.core.commons.activity.IContinuumNodeActivity.NodeActivityOutput
import org.projectcontinuum.core.commons.model.ContinuumWorkflowModel
import org.projectcontinuum.core.commons.model.PortData
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Configurable fake [IContinuumNodeActivity] for driving [ContinuumWorkflow][org.projectcontinuum.core.orchestration.workflow.ContinuumWorkflow]
 * tests without any real node business logic.
 *
 * Configured per-test via [successOutputs], [errorOutputs] and [blockUntil] before starting a workflow.
 */
class FakeContinuumNodeActivity : IContinuumNodeActivity {

  /** Outputs to return for a successful node run, keyed by [ContinuumWorkflowModel.Node.id]. */
  val successOutputs: MutableMap<String, Map<String, PortData>> = ConcurrentHashMap()

  /** Outputs containing the `$error` port to return instead of success, keyed by [ContinuumWorkflowModel.Node.id]. */
  val errorOutputs: MutableMap<String, Map<String, PortData>> = ConcurrentHashMap()

  /** If present for a graph node id, run() blocks (bounded) on this latch before returning. */
  val blockUntil: MutableMap<String, CountDownLatch> = ConcurrentHashMap()

  /** Graph node ids this fake was invoked for, in call order. */
  val invokedNodeIds: MutableList<String> = Collections.synchronizedList(mutableListOf())

  override fun run(
    node: ContinuumWorkflowModel.Node,
    inputs: Map<String, PortData>
  ): NodeActivityOutput {
    invokedNodeIds.add(node.id)
    blockUntil[node.id]?.await(5, TimeUnit.SECONDS)

    errorOutputs[node.id]?.let { return NodeActivityOutput(node.id, it) }
    return NodeActivityOutput(node.id, successOutputs[node.id] ?: emptyMap())
  }
}
