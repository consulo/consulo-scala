package org.jetbrains.plugins.scala.shared.util
final case class CompilationId(timestamp: Long, documentVersions: Map[CanonicalPath, Long] & Serializable)
  extends Serializable
