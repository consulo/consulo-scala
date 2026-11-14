package org.jetbrains.sbt.project

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import consulo.project.Project

import java.util.concurrent.ConcurrentHashMap

// upstream: @Service(Array(Service.Level.PROJECT))
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
private final class GeneratedManagedSourcesService {
  private val generatedForPath: ConcurrentHashMap[String, java.lang.Boolean] = new ConcurrentHashMap()

  def generatedForPath(path: String): Boolean =
    generatedForPath.getOrDefault(path, java.lang.Boolean.FALSE)

  def setGeneratedForPath(path: String, value: Boolean): Unit = {
    generatedForPath.put(path, value)
  }

  def generatedForAnyPath: Boolean =
    generatedForPath.containsValue(java.lang.Boolean.TRUE)
}

private object GeneratedManagedSourcesService {
  def instance(project: Project): GeneratedManagedSourcesService =
    project.getInstance(classOf[GeneratedManagedSourcesService])
}
