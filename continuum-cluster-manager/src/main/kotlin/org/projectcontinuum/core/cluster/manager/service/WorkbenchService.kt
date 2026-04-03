package org.projectcontinuum.core.cluster.manager.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import freemarker.template.Configuration
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.KubernetesClient
import org.projectcontinuum.core.cluster.manager.entity.WorkbenchInstanceEntity
import org.projectcontinuum.core.cluster.manager.exception.WorkbenchNotFoundException
import org.projectcontinuum.core.cluster.manager.model.*
import org.projectcontinuum.core.cluster.manager.repository.WorkbenchInstanceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.StringWriter
import java.time.Instant
import java.util.UUID

@Service
class WorkbenchService(
  private val repository: WorkbenchInstanceRepository,
  private val kubernetesClient: KubernetesClient,
  private val freemarkerConfig: Configuration
) {

  private val logger = LoggerFactory.getLogger(WorkbenchService::class.java)
  private val objectMapper = jacksonObjectMapper()

  fun createWorkbench(userId: String, request: WorkbenchCreateRequest): WorkbenchResponse {
    val existing = repository.findByUserIdAndInstanceName(userId, request.instanceName)
    if (existing != null && existing.status != WorkbenchStatus.TERMINATING.name) {
      throw IllegalArgumentException("Workbench '${request.instanceName}' already exists for user '$userId'")
    }

    val instanceId = UUID.randomUUID()
    val now = Instant.now()

    var entity = WorkbenchInstanceEntity(
      instanceId = instanceId,
      instanceName = request.instanceName,
      namespace = request.namespace,
      userId = userId,
      status = WorkbenchStatus.PENDING.name,
      image = request.image,
      cpuRequest = request.resources.cpuRequest,
      cpuLimit = request.resources.cpuLimit,
      memoryRequest = request.resources.memoryRequest,
      memoryLimit = request.resources.memoryLimit,
      storageSize = request.resources.storageSize,
      storageClassName = request.resources.storageClassName,
      createdAt = now,
      updatedAt = now
    )

    entity = repository.save(entity)

    val templateModel = buildTemplateModel(entity)
    val k8sResourceIds = mutableListOf<String>()

    try {
      val pvcYaml = renderTemplate("pvc.ftl", templateModel)
      applyYaml(pvcYaml, request.namespace)
      k8sResourceIds.add("persistentvolumeclaim/wb-${instanceId}-pvc")

      val deploymentYaml = renderTemplate("deployment.ftl", templateModel)
      applyYaml(deploymentYaml, request.namespace)
      k8sResourceIds.add("deployment/wb-${instanceId}-deployment")

      val serviceYaml = renderTemplate("service.ftl", templateModel)
      applyYaml(serviceYaml, request.namespace)
      k8sResourceIds.add("service/wb-${instanceId}-svc")

      val updatedEntity = entity.copy(
        status = WorkbenchStatus.RUNNING.name,
        k8sResources = objectMapper.writeValueAsString(k8sResourceIds),
        updatedAt = Instant.now()
      )
      repository.save(updatedEntity)

      return toResponse(updatedEntity)
    } catch (ex: Exception) {
      logger.error("Failed to create K8s resources for workbench $instanceId", ex)
      val failedEntity = entity.copy(
        status = WorkbenchStatus.FAILED.name,
        k8sResources = objectMapper.writeValueAsString(k8sResourceIds),
        updatedAt = Instant.now()
      )
      repository.save(failedEntity)
      throw ex
    }
  }

  fun getWorkbenchStatus(userId: String, instanceName: String, namespace: String?): WorkbenchResponse {
    val entity = if (namespace != null) {
      repository.findByUserId(userId)
        .firstOrNull { it.instanceName == instanceName && it.namespace == namespace }
    } else {
      repository.findByUserIdAndInstanceName(userId, instanceName)
    } ?: throw WorkbenchNotFoundException("Workbench '$instanceName' not found for user '$userId'")

    val refreshedEntity = refreshStatusFromK8s(entity)
    return toResponse(refreshedEntity)
  }

  fun deleteWorkbench(userId: String, instanceName: String, namespace: String?) {
    val entity = if (namespace != null) {
      repository.findByUserId(userId)
        .firstOrNull { it.instanceName == instanceName && it.namespace == namespace }
    } else {
      repository.findByUserIdAndInstanceName(userId, instanceName)
    } ?: throw WorkbenchNotFoundException("Workbench '$instanceName' not found for user '$userId'")

    val terminatingEntity = entity.copy(
      status = WorkbenchStatus.TERMINATING.name,
      updatedAt = Instant.now()
    )
    val saved = repository.save(terminatingEntity)

    try {
      deleteK8sResourcesByLabel(entity.instanceId.toString(), entity.namespace)
    } catch (ex: Exception) {
      logger.error("Failed to delete K8s resources for workbench ${entity.instanceId}", ex)
    }

    val deletedEntity = saved.copy(
      status = WorkbenchStatus.DELETED.name,
      updatedAt = Instant.now()
    )
    repository.save(deletedEntity)
  }

  fun listWorkbenches(userId: String, namespace: String?): List<WorkbenchResponse> {
    val entities = if (namespace != null) {
      repository.findByUserIdAndNamespace(userId, namespace)
    } else {
      repository.findByUserId(userId)
    }
    return entities.map { toResponse(it) }
  }

  fun suspendWorkbench(userId: String, instanceName: String, namespace: String?): WorkbenchResponse {
    val entity = if (namespace != null) {
      repository.findByUserId(userId)
        .firstOrNull { it.instanceName == instanceName && it.namespace == namespace }
    } else {
      repository.findByUserIdAndInstanceName(userId, instanceName)
    } ?: throw WorkbenchNotFoundException("Workbench '$instanceName' not found for user '$userId'")

    if (entity.status == WorkbenchStatus.SUSPENDED.name) {
      throw IllegalArgumentException("Workbench '$instanceName' is already suspended")
    }

    try {
      suspendK8sResources(entity.instanceId.toString(), entity.namespace)
    } catch (ex: Exception) {
      logger.error("Failed to suspend K8s resources for workbench ${entity.instanceId}", ex)
    }

    val suspendedEntity = entity.copy(
      status = WorkbenchStatus.SUSPENDED.name,
      updatedAt = Instant.now()
    )
    repository.save(suspendedEntity)

    return toResponse(suspendedEntity)
  }

  fun resumeWorkbench(userId: String, instanceName: String, namespace: String?): WorkbenchResponse {
    val entity = if (namespace != null) {
      repository.findByUserId(userId)
        .firstOrNull { it.instanceName == instanceName && it.namespace == namespace }
    } else {
      repository.findByUserIdAndInstanceName(userId, instanceName)
    } ?: throw WorkbenchNotFoundException("Workbench '$instanceName' not found for user '$userId'")

    if (entity.status != WorkbenchStatus.SUSPENDED.name) {
      throw IllegalArgumentException("Workbench '$instanceName' is not suspended")
    }

    val templateModel = buildTemplateModel(entity)

    try {
      val deploymentYaml = renderTemplate("deployment.ftl", templateModel)
      applyYaml(deploymentYaml, entity.namespace)

      val serviceYaml = renderTemplate("service.ftl", templateModel)
      applyYaml(serviceYaml, entity.namespace)
    } catch (ex: Exception) {
      logger.error("Failed to resume K8s resources for workbench ${entity.instanceId}", ex)
      throw ex
    }

    val resumedEntity = entity.copy(
      status = WorkbenchStatus.RUNNING.name,
      updatedAt = Instant.now()
    )
    repository.save(resumedEntity)

    return toResponse(resumedEntity)
  }

  fun updateWorkbench(userId: String, instanceName: String, namespace: String?, request: WorkbenchUpdateRequest): WorkbenchResponse {
    val entity = if (namespace != null) {
      repository.findByUserId(userId)
        .firstOrNull { it.instanceName == instanceName && it.namespace == namespace }
    } else {
      repository.findByUserIdAndInstanceName(userId, instanceName)
    } ?: throw WorkbenchNotFoundException("Workbench '$instanceName' not found for user '$userId'")

    val updatedEntity = entity.copy(
      image = request.image ?: entity.image,
      cpuRequest = request.resources?.cpuRequest ?: entity.cpuRequest,
      cpuLimit = request.resources?.cpuLimit ?: entity.cpuLimit,
      memoryRequest = request.resources?.memoryRequest ?: entity.memoryRequest,
      memoryLimit = request.resources?.memoryLimit ?: entity.memoryLimit,
      storageSize = request.resources?.storageSize ?: entity.storageSize,
      storageClassName = request.resources?.storageClassName ?: entity.storageClassName,
      updatedAt = Instant.now()
    )

    repository.save(updatedEntity)

    val templateModel = buildTemplateModel(updatedEntity)

    val deploymentYaml = renderTemplate("deployment.ftl", templateModel)
    applyYaml(deploymentYaml, updatedEntity.namespace)

    val serviceYaml = renderTemplate("service.ftl", templateModel)
    applyYaml(serviceYaml, updatedEntity.namespace)

    return toResponse(updatedEntity)
  }

  private fun refreshStatusFromK8s(entity: WorkbenchInstanceEntity): WorkbenchInstanceEntity {
    return try {
      val deployment = kubernetesClient.apps().deployments()
        .inNamespace(entity.namespace)
        .withName("wb-${entity.instanceId}-deployment")
        .get()

      val newStatus = if (deployment == null) {
        WorkbenchStatus.UNKNOWN.name
      } else {
        val readyReplicas = deployment.status?.readyReplicas ?: 0
        if (readyReplicas > 0) WorkbenchStatus.RUNNING.name else WorkbenchStatus.PENDING.name
      }

      if (newStatus != entity.status) {
        val updated = entity.copy(status = newStatus, updatedAt = Instant.now())
        repository.save(updated)
        updated
      } else {
        entity
      }
    } catch (ex: Exception) {
      logger.warn("Could not refresh K8s status for workbench ${entity.instanceId}", ex)
      entity
    }
  }

  private fun deleteK8sResourcesByLabel(instanceId: String, namespace: String) {
    val labels = mapOf(
      "instance-id" to instanceId,
      "app" to "continuum-workbench",
      "managed-by" to "continuum-cluster-manager"
    )

    kubernetesClient.apps().deployments()
      .inNamespace(namespace).withLabels(labels).delete()
    kubernetesClient.services()
      .inNamespace(namespace).withLabels(labels).delete()
    kubernetesClient.persistentVolumeClaims()
      .inNamespace(namespace).withLabels(labels).delete()
  }

  private fun suspendK8sResources(instanceId: String, namespace: String) {
    val labels = mapOf(
      "instance-id" to instanceId,
      "app" to "continuum-workbench",
      "managed-by" to "continuum-cluster-manager"
    )

    kubernetesClient.apps().deployments()
      .inNamespace(namespace).withLabels(labels).delete()
    kubernetesClient.services()
      .inNamespace(namespace).withLabels(labels).delete()
  }

  private fun renderTemplate(templateName: String, model: Map<String, Any?>): String {
    val template = freemarkerConfig.getTemplate(templateName)
    val writer = StringWriter()
    template.process(model, writer)
    return writer.toString()
  }

  @Suppress("DEPRECATION")
  private fun applyYaml(yaml: String, namespace: String) {
    val resources: List<HasMetadata> = kubernetesClient.load(yaml.byteInputStream()).items()
    for (resource in resources) {
      kubernetesClient.resource(resource)
        .inNamespace(namespace)
        .createOrReplace()
    }
  }

  private fun buildTemplateModel(entity: WorkbenchInstanceEntity): Map<String, Any?> {
    return mapOf(
      "instanceId" to entity.instanceId.toString(),
      "namespace" to entity.namespace,
      "image" to entity.image,
      "cpuRequest" to entity.cpuRequest,
      "cpuLimit" to entity.cpuLimit,
      "memoryRequest" to entity.memoryRequest,
      "memoryLimit" to entity.memoryLimit,
      "storageSize" to entity.storageSize,
      "storageClassName" to (entity.storageClassName ?: "")
    )
  }

  private fun toResponse(entity: WorkbenchInstanceEntity): WorkbenchResponse {
    return WorkbenchResponse(
      instanceId = entity.instanceId,
      instanceName = entity.instanceName,
      namespace = entity.namespace,
      userId = entity.userId,
      status = entity.status,
      image = entity.image,
      resources = ResourceSpec(
        cpuRequest = entity.cpuRequest,
        cpuLimit = entity.cpuLimit,
        memoryRequest = entity.memoryRequest,
        memoryLimit = entity.memoryLimit,
        storageSize = entity.storageSize,
        storageClassName = entity.storageClassName
      ),
      serviceEndpoint = "wb-${entity.instanceId}-svc.${entity.namespace}.svc.cluster.local:8080",
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt
    )
  }
}
