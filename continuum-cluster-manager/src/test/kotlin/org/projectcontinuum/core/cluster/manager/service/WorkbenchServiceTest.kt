package org.projectcontinuum.core.cluster.manager.service

import freemarker.template.Configuration
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectcontinuum.core.cluster.manager.entity.WorkbenchInstanceEntity
import org.projectcontinuum.core.cluster.manager.exception.WorkbenchNotFoundException
import org.projectcontinuum.core.cluster.manager.model.*
import org.projectcontinuum.core.cluster.manager.repository.WorkbenchInstanceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID

@SpringBootTest
@EnableKubernetesMockClient(crud = true)
@ActiveProfiles("test")
class WorkbenchServiceTest {

  @Autowired
  private lateinit var repository: WorkbenchInstanceRepository

  lateinit var client: KubernetesClient
  lateinit var server: KubernetesMockServer

  private lateinit var service: WorkbenchService

  @BeforeEach
  fun setUp() {
    repository.deleteAll()

    val freemarkerConfig = Configuration(Configuration.VERSION_2_3_34)
    freemarkerConfig.setClassLoaderForTemplateLoading(this::class.java.classLoader, "/templates")
    freemarkerConfig.defaultEncoding = "UTF-8"

    service = WorkbenchService(repository, client, freemarkerConfig)
  }

  @Test
  fun `createWorkbench saves entity and creates K8s resources`() {
    val request = WorkbenchCreateRequest(
      instanceName = "test-wb",
      namespace = "default",
      resources = ResourceSpec(),
      image = "theiaide/theia:latest"
    )

    val response = service.createWorkbench("user-1", request)

    assertEquals("test-wb", response.instanceName)
    assertEquals("user-1", response.userId)
    assertEquals("default", response.namespace)
    assertEquals(WorkbenchStatus.RUNNING.name, response.status)
    assertNotNull(response.instanceId)
    assertNotNull(response.serviceEndpoint)

    val saved = repository.findByUserIdAndInstanceName("user-1", "test-wb")
    assertNotNull(saved)
    assertEquals(response.instanceId, saved!!.instanceId)

    val deployments = client.apps().deployments().inNamespace("default")
      .withLabel("instance-id", response.instanceId.toString()).list().items
    assertEquals(1, deployments.size)

    val services = client.services().inNamespace("default")
      .withLabel("instance-id", response.instanceId.toString()).list().items
    assertEquals(1, services.size)

    val pvcs = client.persistentVolumeClaims().inNamespace("default")
      .withLabel("instance-id", response.instanceId.toString()).list().items
    assertEquals(1, pvcs.size)
  }

  @Test
  fun `getWorkbenchStatus returns response for existing workbench`() {
    val instanceId = UUID.randomUUID()
    val entity = WorkbenchInstanceEntity(
      instanceId = instanceId,
      instanceName = "status-wb",
      namespace = "default",
      userId = "user-1",
      status = WorkbenchStatus.RUNNING.name,
      image = "theiaide/theia:latest",
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )
    repository.save(entity)

    val response = service.getWorkbenchStatus("user-1", "status-wb", null)

    assertEquals("status-wb", response.instanceName)
    assertEquals("user-1", response.userId)
  }

  @Test
  fun `getWorkbenchStatus throws WorkbenchNotFoundException for missing workbench`() {
    assertThrows<WorkbenchNotFoundException> {
      service.getWorkbenchStatus("user-1", "nonexistent", null)
    }
  }

  @Test
  fun `deleteWorkbench soft-deletes DB record and removes K8s resources`() {
    val request = WorkbenchCreateRequest(
      instanceName = "delete-wb",
      namespace = "default"
    )
    val created = service.createWorkbench("user-1", request)

    service.deleteWorkbench("user-1", "delete-wb", null)

    // Should not appear in active queries
    assertNull(repository.findByUserIdAndInstanceName("user-1", "delete-wb"))

    // But record still exists in DB with DELETED status
    val dbRecord = repository.findById(created.instanceId)
    assertTrue(dbRecord.isPresent)
    assertEquals("DELETED", dbRecord.get().status)

    val deployments = client.apps().deployments().inNamespace("default")
      .withLabel("instance-id", created.instanceId.toString()).list().items
    assertEquals(0, deployments.size)
  }

  @Test
  fun `listWorkbenches returns all workbenches for user`() {
    service.createWorkbench("user-1", WorkbenchCreateRequest(instanceName = "wb-1"))
    service.createWorkbench("user-1", WorkbenchCreateRequest(instanceName = "wb-2"))
    service.createWorkbench("user-2", WorkbenchCreateRequest(instanceName = "wb-3"))

    val user1Workbenches = service.listWorkbenches("user-1", null)
    assertEquals(2, user1Workbenches.size)

    val user2Workbenches = service.listWorkbenches("user-2", null)
    assertEquals(1, user2Workbenches.size)
  }

  @Test
  fun `listWorkbenches filters by namespace`() {
    service.createWorkbench("user-1", WorkbenchCreateRequest(instanceName = "wb-dev", namespace = "dev"))
    service.createWorkbench("user-1", WorkbenchCreateRequest(instanceName = "wb-prod", namespace = "prod"))

    val devWorkbenches = service.listWorkbenches("user-1", "dev")
    assertEquals(1, devWorkbenches.size)
    assertEquals("wb-dev", devWorkbenches[0].instanceName)
  }

  @Test
  fun `updateWorkbench updates image and resources`() {
    service.createWorkbench("user-1", WorkbenchCreateRequest(instanceName = "update-wb"))

    val updateRequest = WorkbenchUpdateRequest(
      image = "theiaide/theia:next",
      resources = ResourceSpec(cpuRequest = "1", memoryRequest = "1Gi")
    )
    val updated = service.updateWorkbench("user-1", "update-wb", null, updateRequest)

    assertEquals("theiaide/theia:next", updated.image)
    assertEquals("1", updated.resources.cpuRequest)
    assertEquals("1Gi", updated.resources.memoryRequest)
  }

  @Test
  fun `updateWorkbench throws WorkbenchNotFoundException for missing workbench`() {
    assertThrows<WorkbenchNotFoundException> {
      service.updateWorkbench("user-1", "nonexistent", null, WorkbenchUpdateRequest())
    }
  }
}
