package org.projectcontinuum.core.cluster.manager.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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
    status: String = WorkbenchStatus.RUNNING.name
  ) = WorkbenchResponse(
    instanceId = UUID.randomUUID(),
    instanceName = instanceName,
    namespace = "default",
    userId = "user-1",
    status = status,
    image = "theiaide/theia:latest",
    resources = ResourceSpec(),
    serviceEndpoint = "wb-test-svc.default.svc.cluster.local:8080",
    createdAt = Instant.now(),
    updatedAt = Instant.now()
  )

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
      .andExpect(jsonPath("$.error").exists())
  }

  @Test
  fun `DELETE returns 204`() {
    mockMvc.perform(
      delete("/api/v1/workbench/my-workbench")
        .header("x-continuum-user-id", "user-1")
    )
      .andExpect(status().isNoContent)
  }

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
  fun `POST without user-id header defaults to anonymous`() {
    val response = sampleResponse(status = WorkbenchStatus.RUNNING.name)
    whenever(workbenchService.createWorkbench(eq("anonymous"), any())).thenReturn(response)

    mockMvc.perform(
      post("/api/v1/workbench")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"instanceName": "my-workbench"}""")
    )
      .andExpect(status().isCreated)
  }
}
