package org.projectcontinuum.core.cluster.manager.controller

import org.projectcontinuum.core.cluster.manager.model.WorkbenchCreateRequest
import org.projectcontinuum.core.cluster.manager.model.WorkbenchResponse
import org.projectcontinuum.core.cluster.manager.model.WorkbenchUpdateRequest
import org.projectcontinuum.core.cluster.manager.service.WorkbenchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/workbench")
class WorkbenchController(
  private val workbenchService: WorkbenchService
) {

  @PostMapping
  fun createWorkbench(
    @RequestHeader("x-continuum-user-id", required = false, defaultValue = "anonymous") userId: String,
    @RequestBody request: WorkbenchCreateRequest
  ): ResponseEntity<WorkbenchResponse> {
    val response = workbenchService.createWorkbench(userId, request)
    return ResponseEntity.status(HttpStatus.CREATED).body(response)
  }

  @GetMapping("/{instanceName}")
  fun getWorkbenchStatus(
    @RequestHeader("x-continuum-user-id", required = false, defaultValue = "anonymous") userId: String,
    @PathVariable instanceName: String,
    @RequestParam(required = false) namespace: String?
  ): ResponseEntity<WorkbenchResponse> {
    val response = workbenchService.getWorkbenchStatus(userId, instanceName, namespace)
    return ResponseEntity.ok(response)
  }

  @DeleteMapping("/{instanceName}")
  fun deleteWorkbench(
    @RequestHeader("x-continuum-user-id", required = false, defaultValue = "anonymous") userId: String,
    @PathVariable instanceName: String,
    @RequestParam(required = false) namespace: String?
  ): ResponseEntity<Void> {
    workbenchService.deleteWorkbench(userId, instanceName, namespace)
    return ResponseEntity.noContent().build()
  }

  @GetMapping
  fun listWorkbenches(
    @RequestHeader("x-continuum-user-id", required = false, defaultValue = "anonymous") userId: String,
    @RequestParam(required = false) namespace: String?
  ): ResponseEntity<List<WorkbenchResponse>> {
    val response = workbenchService.listWorkbenches(userId, namespace)
    return ResponseEntity.ok(response)
  }

  @PutMapping("/{instanceName}/suspend")
  fun suspendWorkbench(
    @RequestHeader("x-continuum-user-id", required = false, defaultValue = "anonymous") userId: String,
    @PathVariable instanceName: String,
    @RequestParam(required = false) namespace: String?
  ): ResponseEntity<WorkbenchResponse> {
    val response = workbenchService.suspendWorkbench(userId, instanceName, namespace)
    return ResponseEntity.ok(response)
  }

  @PutMapping("/{instanceName}/resume")
  fun resumeWorkbench(
    @RequestHeader("x-continuum-user-id", required = false, defaultValue = "anonymous") userId: String,
    @PathVariable instanceName: String,
    @RequestParam(required = false) namespace: String?
  ): ResponseEntity<WorkbenchResponse> {
    val response = workbenchService.resumeWorkbench(userId, instanceName, namespace)
    return ResponseEntity.ok(response)
  }

  @PutMapping("/{instanceName}")
  fun updateWorkbench(
    @RequestHeader("x-continuum-user-id", required = false, defaultValue = "anonymous") userId: String,
    @PathVariable instanceName: String,
    @RequestParam(required = false) namespace: String?,
    @RequestBody request: WorkbenchUpdateRequest
  ): ResponseEntity<WorkbenchResponse> {
    val response = workbenchService.updateWorkbench(userId, instanceName, namespace, request)
    return ResponseEntity.ok(response)
  }
}
