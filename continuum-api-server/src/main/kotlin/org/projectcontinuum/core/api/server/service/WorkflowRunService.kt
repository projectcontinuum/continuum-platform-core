package org.projectcontinuum.core.api.server.service

import io.github.perplexhub.rsql.RSQLJPASupport
import org.projectcontinuum.core.api.server.entity.jpa.WorkflowRunEntity
import org.projectcontinuum.core.api.server.repository.jpa.WorkflowRunRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WorkflowRunService(
  val workflowRunRepository: WorkflowRunRepository
) {

  private fun ownedBySpec(ownedBy: String): Specification<WorkflowRunEntity> {
    return Specification { root, _, cb -> cb.equal(root.get<String>("ownedBy"), ownedBy) }
  }

  fun findAll(ownedBy: String, filter: String?, page: Int, size: Int, sort: String?): Page<WorkflowRunEntity> {
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

    var spec: Specification<WorkflowRunEntity> = Specification.where(ownedBySpec(ownedBy))
    if (!filter.isNullOrBlank()) {
      spec = spec.and(RSQLJPASupport.toSpecification(filter))
    }
    if (!sort.isNullOrBlank()) {
      spec = spec.and(RSQLJPASupport.toSort(sort))
    }

    return workflowRunRepository.findAll(spec, pageable)
  }

  fun findById(ownedBy: String, workflowId: UUID): WorkflowRunEntity? {
    val spec = Specification.where(ownedBySpec(ownedBy))
      .and(Specification<WorkflowRunEntity> { root, _, cb -> cb.equal(root.get<UUID>("workflowId"), workflowId) })
    return workflowRunRepository.findOne(spec).orElse(null)
  }

  fun count(ownedBy: String, filter: String?): Long {
    var spec: Specification<WorkflowRunEntity> = Specification.where(ownedBySpec(ownedBy))
    if (!filter.isNullOrBlank()) {
      spec = spec.and(RSQLJPASupport.toSpecification(filter))
    }
    return workflowRunRepository.count(spec)
  }
}
