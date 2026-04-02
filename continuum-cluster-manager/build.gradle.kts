plugins {
  kotlin("jvm") version "2.1.0"
  kotlin("plugin.spring") version "1.9.25"
  id("org.springframework.boot") version "3.4.0"
  id("io.spring.dependency-management") version "1.1.6"
  id("com.google.cloud.tools.jib") version "3.4.1"
}

group = "org.projectcontinuum.core"
description = "Continuum cluster management API Server — REST API for managing and monitoring continuum cluster resources"
version = property("platformVersion").toString()

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  // Swagger-UI Dependencies
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

  // Kubernetes client
  implementation("io.fabric8:kubernetes-client:7.1.0")

  // Template engine for K8s manifests
  implementation("org.freemarker:freemarker:2.3.34")

  // JSON
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

  // Database
  implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
  implementation("org.postgresql:postgresql")

  // Test dependencies
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
  testImplementation("io.fabric8:kubernetes-server-mock:7.1.0")
  testImplementation("com.h2database:h2")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

jib {
  from {
    image = "eclipse-temurin:21-jre"
  }

  to {
    image = "docker.io/projectcontinuum/${project.name.lowercase()}:${project.version}"
    auth {
      username = System.getenv("DOCKER_REPO_USERNAME") ?: ""
      password = System.getenv("DOCKER_REPO_PASSWORD") ?: ""
    }
  }
}
