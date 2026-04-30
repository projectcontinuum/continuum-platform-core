package org.projectcontinuum.core.commons.context

/**
 * Thread-local holder for resolved credentials during node execution.
 *
 * Before a node's `execute()` method is called, the worker framework resolves any credential
 * fields declared in the node's UI Schema and stores the results here. Node developers can
 * then retrieve their credentials without needing any changes to the `execute()` method signature.
 *
 * The credentials map is structured as: `label -> credential data map`.
 * - The label comes from `options.credentialLabel` in the UI Schema (or falls back to the property name)
 * - The credential data map contains the key-value pairs from the credential (e.g., accessKeyId, secretAccessKey)
 *
 * ## Example usage inside a node's execute() method:
 * ```kotlin
 * override fun execute(
 *     properties: Map<String, Any>?,
 *     inputs: Map<String, NodeInputReader>,
 *     nodeOutputWriter: NodeOutputWriter,
 *     nodeProgressCallback: NodeProgressCallback
 * ) {
 *     // Retrieve credentials by the label defined in your UI Schema
 *     val awsCreds = CredentialContext.get("AWS Credentials")
 *         ?: throw NodeRuntimeException(workflowId = "", nodeId = "", message = "AWS credentials not found")
 *
 *     val accessKey = awsCreds["accessKeyId"] as String
 *     val secretKey = awsCreds["secretAccessKey"] as String
 *
 *     // Use credentials to connect to AWS, etc.
 * }
 * ```
 */
object CredentialContext {

  private val threadLocal = ThreadLocal<Map<String, Map<String, Any>>>()

  /** Sets the resolved credentials map for the current thread */
  fun set(credentials: Map<String, Map<String, Any>>) {
    threadLocal.set(credentials)
  }

  /** Returns all resolved credentials, or an empty map if none were resolved */
  fun getAll(): Map<String, Map<String, Any>> = threadLocal.get() ?: emptyMap()

  /** Returns the credential data for the given label, or null if not found */
  fun get(label: String): Map<String, Any>? = threadLocal.get()?.get(label)

  /** Clears the credentials from the current thread to prevent leaks */
  fun clear() {
    threadLocal.remove()
  }
}
