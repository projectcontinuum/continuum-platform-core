package org.projectcontinuum.core.credentials.config

import org.projectcontinuum.core.credentials.repository.CredentialRepository
import org.projectcontinuum.core.credentials.repository.PostgresCredentialRepository
import org.projectcontinuum.core.credentials.repository.SpringDataCredentialRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RepositoryConfig {

  @Bean
  @ConditionalOnProperty(
    name = ["continuum.core.credentials.storage-backend"],
    havingValue = "postgres",
    matchIfMissing = true
  )
  fun postgresCredentialRepository(
    springDataRepository: SpringDataCredentialRepository
  ): CredentialRepository = PostgresCredentialRepository(springDataRepository)
}
