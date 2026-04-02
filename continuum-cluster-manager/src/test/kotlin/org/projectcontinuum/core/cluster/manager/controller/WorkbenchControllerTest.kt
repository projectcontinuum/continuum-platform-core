package org.projectcontinuum.core.cluster.manager.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.fabric8.kubernetes.client.KubernetesClientException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.projectcontinuum.core.cluster.manager.exception.WorkbenchNotFoundException
import org.projectcontinuum.core.cluster.manager.model.ResourceSpec
import org.projectcontinuum.core.cluster.manager.model.WorkbenchResponse
import org.projectcontinuum.core.cluster.manager.model.WorkbenchStatus
import org.projectcontinuum.core.cluster.manager.service.WorkbenchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

@WebMvcTest(WorkbenchController::class)
class WorkbenchControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockitoBean
  private lateinit var workbenchService: WorkbenchService

  private val objectMapper = jacksonObjectMapper()

  private fun sampleResponse(
    instanceName: String = "my-workbench",
    status: String = WorkbenchStatus.RUNNING.name,
    namespace: String = "default",
    userId: String = "user-1"
  ) = WorkbenchResponse(
    instanceId = UUID.randomUUID(),
    instanceName = instanceName,
    namespace = namespace,
    userId = userId,
    status = status,
    image = "theiaide/theia:latest",
    resources = ResourceSpec(),
    serviceEndpoint = "wb-test-svc.$namespace.svc.cluster.local:8080",
    createdAt = Instant.now(),
    updatedAt = Instant.now()
  )

  // ── POST /api/v1/workbench ──────────────────────────────────────────

  @Test
  fun `POST creates workbench and returns 201`() {
    val response = sampleResponse()
    whenever(workbenchService.createWorkbench(eq("user-1"), any())).thenReturn(response)

    mockMvc.perform(
      post("/api/v1/workbench")
        .header("x-continuum-user-id", "user-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"instanceName": "my-workbench", "namespace": "default"}""")
    )
      .andExpect(status().isCreated)
      .andExpect(jsonPath("$.instanceName").value("my-workbench"))
      .andExpect(jsonPath("$.status").value("RUNNING"))
  }

  @Test
  fun `POST without user-id header defaults to anonymous`() {
    val response = sampleResponse(userId = "anonymous")
    whenever(workbenchService.createWorkbench(eq("anonymous"), any())).thenReturn(response)

    mockMvc.perform(
      post("/api/v1/workbench")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"instanceName": "my-workbench"}""")
    )
      .andExpect(status().isCreated)
  }

  @Test
  fun `POST returns 400 when workbench already exists`() {
    whenever(workbenchService.createWorkbench(eq("user-1"), any()))
      .thenThrow(IllegalArgumentException("Workbench 'my-workbench' already exists for user 'user-1'"))

    mockMvc.perform(
      post("/api/v1/workbench")
        .header("x-continuum-user-id", "user-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"instanceName": "my-workbench"}""")
    )
      .andExpect(status().isBadRequest)
      .andExpect(jsonPath("$.error").value("Workbench 'my-workbench' already exists for user 'user-1'"))
  }

  @Test
  fun `POST returns 201 with full resource spec`() {
    val response = sampleResponse()
    whenever(workbenchService.createWorkbench(eq("user-1"), any())).thenReturn(response)

    mockMvc.perform(
      post("/api/v1/workbench")
        .header("x-continuum-user-id", "user-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{
          "instanceName": "my-workbench",
          "namespace": "dev",
          "image": "theiaide/theia:next",
          "resources": {
            "cpuRequest": "1",
            "cpuLimit": "4",
            "memoryRequest": "1Gi",
            "memoryLimit": "4Gi",
            "storageSize": "10Gi",
            "storageClassName": "fast-ssd"
          }
        }""")
    )
      .andExpect(status().isCreated)
      .andExpect(jsonPath("$.instanceName").value("my-workbench"))
  }

  @Test
  fun `POST returns 500 when K8s client fails`() {
    whenever(workbenchService.createWorkbench(eq("user-1"), any()))
      .thenThrow(KubernetesClientException("connection refused"))

    mockMvc.perform(
      post("/api/v1/workbench")
        .header("x-continuum-user-id", "user-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"instanceName": "my-workbench"}""")
    )
      .andExpect(status().isInternalServerError)
      .andExpect(jsonPath("$.error").value("Kubernetes operation failed: connection refused"))
  }

  @Test
  fun `POST returns 500 on unexpected exception`() {
    whenever(workbenchService.createWorkbench(eq("user-1"), any()))
      .thenThrow(RuntimeException("unexpected"))

    mockMvc.perform(
      post("/api/v1/workbench")
        .header("x-continuum-user-id", "user-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"instanceName": "my-workbench"}""")
    )
      .andExpect(status().isInternalServerError)
      .andExpect(jsonPath("$.error").value("Internal server error"))
  }

  // ── GET /api/v1/workbench/{instanceName} ────────────────────────────

  @Test
  fun `GET instance returns workbench status`() {
    val response = sampleResponse()
    whenever(workbenchService.getWorkbenchStatus("user-1", "my-workbench", null)).thenReturn(response)

    mockMvc.perform(
      get("/api/v1/workbench/my-workbench")
        .header("x-continuum-user-id", "user-1")
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.instanceName").value("my-workbench"))
      .andExpect(jsonPath("$.status").value("RUNNING"))
  }

  @Test
  fun `GET instance with namespace param`() {
    val response = sampleResponse()
    whenever(workbenchService.getWorkbenchStatus("user-1", "my-workbench", "dev")).thenReturn(response)

    mockMvc.perform(
      get("/api/v1/workbench/my-workbench")
        .header("x-continuum-user-id", "user-1")
        .param("namespace", "dev")
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.instanceName").value("my-workbench"))
  }

  @Test
  fun `GET instance returns 404 when not found`() {
    whenever(workbenchService.getWorkbenchStatus("user-1", "missing", null))
      .thenThrow(WorkbenchNotFoundException("Workbench 'missing' not found for user 'user-1'"))

    mockMvc.perform(
      get("/api/v1/workbench/missing")
        .header("x-continuum-user-id", "user-1")
    )
      .andExpect(status().isNotFound)
      .andExpect(jsonPath("$.error").value("Workbench 'missing' not found for user 'user-1'"))
  }

  @Test
  fun `GET instance without user-id header defaults to anonymous`() {
    val response = sampleResponse(userId = "anonymous")
    whenever(workbenchService.getWorkbenchStatus("anonymous", "my-workbench", null)).thenReturn(response)

    mockMvc.perform(
      get("/api/v1/workbench/my-workbench")
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.userId").value("anonymous"))
  }

  @Test
  fun `GET instance returns response with all fields`() {
    val instanceId = UUID.randomUUID()
    val now = Instant.parse("2026-01-15T10:30:00Z")
    val response = WorkbenchResponse(
      instanceId = instanceId,
      instanceName = "full-wb",
      namespace = "staging",
      userId = "user-1",
      status = WorkbenchStatus.PENDING.name,
      image = "theiaide/theia:1.0",
      resources = ResourceSpec(
        cpuRequest = "1",
        cpuLimit = "4",
        memoryRequest = "1Gi",
        memoryLimit = "4Gi",
        storageSize = "20Gi",
        storageClassName = "fast-ssd"
      ),
      serviceEndpoint = "wb-$instanceId-svc.staging.svc.cluster.local:8080",
      createdAt = now,
      updatedAt = now
    )
    whenever(workbenchService.getWorkbenchStatus("user-1", "full-wb", null)).thenReturn(response)

    mockMvc.perform(
      get("/api/v1/workbench/full-wb")
        .header("x-continuum-user-id", "user-1")
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.instanceId").value(instanceId.toString()))
      .andExpect(jsonPath("$.instanceName").value("full-wb"))
      .andExpect(jsonPath("$.namespace").value("staging"))
      .andExpect(jsonPath("$.userId").value("user-1"))
      .andExpect(jsonPath("$.status").value("PENDING"))
      .andExpect(jsonPath("$.image").value("theiaide/theia:1.0"))
      .andExpect(jsonPath("$.resources.cpuRequest").value("1"))
      .andExpect(jsonPath("$.resources.cpuLimit").value("4"))
      .andExpect(jsonPath("$.resources.memoryRequest").value("1Gi"))
      .andExpect(jsonPath("$.resources.memoryLimit").value("4Gi"))
      .andExpect(jsonPath("$.resources.storageSize").value("20Gi"))
      .andExpect(jsonPath("$.resources.storageClassName").value("fast-ssd"))
      .andExpect(jsonPath("$.serviceEndpoint").exists())
  }

  // ── DELETE /api/v1/workbench/{instanceName} ─────────────────────────

  @Test
  fun `DELETE returns 204`() {
    mockMvc.perform(
      delete("/api/v1/workbench/my-workbench")
        .header("x-continuum-user-id", "user-1")
    )
      .andExpect(status().isNoContent)
  }

  @Test
  fun `DELETE with namespace param calls service with namespace`() {
    mockMvc.perform(
      delete("/api/v1/workbench/my-workbench")
        .header("x-continuum-user-id", "user-1")
        .param("namespace", "dev")
    )
      .andExpect(status().isNoContent)

    verify(workbenchService).deleteWorkbench("user-1", "my-workbench", "dev")
  }

  @Test
  fun `DELETE returns 404 when workbench not found`() {
    whenever(workbenchService.deleteWorkbench("user-1", "missing", null))
      .thenThrow(WorkbenchNotFoundException("Workbench 'missing' not found for user 'user-1'"))

    mockMvc.perform(
      delete("/api/v1/workbench/missing")
        .header("x-continuum-user-id", "user-1")
    )
      .andExpect(status().isNotFound)
      .andExpect(jsonPath("$.error").exists())
  }

  @Test
  fun `DELETE without user-id header defaults to anonymous`() {
    mockMvc.perform(
      delete("/api/v1/workbench/my-workbench")
    )
      .andExpect(status().isNoContent)

    verify(workbenchService).deleteWorkbench("anonymous", "my-workbench", null)
  }

  // ── GET /api/v1/workbench ───────────────────────────────────────────

  @Test
  fun `GET list returns workbenches for user`() {
    val responses = listOf(sampleResponse("wb-1"), sampleResponse("wb-2"))
    whenever(workbenchService.listWorkbenches("user-1", null)).thenReturn(responses)

    mockMvc.perform(
      get("/api/v1/workbench")
        .header("x-continuum-user-id", "user-1")
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].instanceName").value("wb-1"))
      .andExpect(jsonPath("$[1].instanceName").value("wb-2"))
  }

  @Test
  fun `GET list with namespace filter`() {
    val responses = listOf(sampleResponse("wb-1"))
    whenever(workbenchService.listWorkbenches("user-1", "production")).thenReturn(responses)

    mockMvc.perform(
      get("/api/v1/workbench")
        .header("x-continuum-user-id", "user-1")
        .param("namespace", "production")
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(1))
  }

  @Test
  fun `GET list returns empty array when no workbenches exist`() {
    whenever(workbenchService.listWorkbenches("user-1", null)).thenReturn(emptyList())

    mockMvc.perform(
      get("/api/v1/workbench")
        .header("x-continuum-user-id", "user-1")
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(0))
  }

  @Test
  fun `GET list without user-id header defaults to anonymous`() {
    whenever(workbenchService.listWorkbenches("anonymous", null)).thenReturn(emptyList())

    mockMvc.perform(
      get("/api/v1/workbench")
    )
      .andExpect(status().isOk)

    verify(workbenchService).listWorkbenches("anonymous", null)
  }

  // ── PUT /api/v1/workbench/{instanceName} ────────────────────────────

  @Test
  fun `PUT updates workbench config`() {
    val response = sampleResponse()
    whenever(workbenchService.updateWorkbench(eq("user-1"), eq("my-workbench"), eq(null), any()))
      .thenReturn(response)

    mockMvc.perform(
      put("/api/v1/workbench/my-workbench")
        .header("x-continuum-user-id", "user-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"image": "theiaide/theia:next"}""")
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.instanceName").value("my-workbench"))
  }

  @Test
  fun `PUT with namespace param calls service with namespace`() {
    val response = sampleResponse()
    whenever(workbenchService.updateWorkbench(eq("user-1"), eq("my-workbench"), eq("staging"), any()))
      .thenReturn(response)

    mockMvc.perform(
      put("/api/v1/workbench/my-workbench")
        .header("x-continuum-user-id", "user-1")
        .param("namespace", "staging")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"image": "theiaide/theia:next"}""")
    )
      .andExpect(status().isOk)

    verify(workbenchService).updateWorkbench(eq("user-1"), eq("my-workbench"), eq("staging"), any())
  }

  @Test
  fun `PUT returns 404 when workbench not found`() {
    whenever(workbenchService.updateWorkbench(eq("user-1"), eq("missing"), eq(null), any()))
      .thenThrow(WorkbenchNotFoundException("Workbench 'missing' not found for user 'user-1'"))

    mockMvc.perform(
      put("/api/v1/workbench/missing")
        .header("x-continuum-user-id", "user-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"image": "theiaide/theia:next"}""")
    )
      .andExpect(status().isNotFound)
      .andExpect(jsonPath("$.error").exists())
  }

  @Test
  fun `PUT with resource spec updates`() {
    val response = sampleResponse()
    whenever(workbenchService.updateWorkbench(eq("user-1"), eq("my-workbench"), eq(null), any()))
      .thenReturn(response)

    mockMvc.perform(
      put("/api/v1/workbench/my-workbench")
        .header("x-continuum-user-id", "user-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{
          "resources": {
            "cpuRequest": "2",
            "cpuLimit": "8",
            "memoryRequest": "2Gi",
            "memoryLimit": "8Gi",
            "storageSize": "50Gi"
          }
        }""")
    )
      .andExpect(status().isOk)
  }

  @Test
  fun `PUT without user-id header defaults to anonymous`() {
    val response = sampleResponse(userId = "anonymous")
    whenever(workbenchService.updateWorkbench(eq("anonymous"), eq("my-workbench"), eq(null), any()))
      .thenReturn(response)

    mockMvc.perform(
      put("/api/v1/workbench/my-workbench")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"image": "theiaide/theia:next"}""")
    )
      .andExpect(status().isOk)

    verify(workbenchService).updateWorkbench(eq("anonymous"), eq("my-workbench"), eq(null), any())
  }
}
