package org.jetbrains.sbt.api.resolvers

import consulo.util.io.FileUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.api.NlsString

sealed trait SbtResolver extends Serializable {
  def name: String
  def root: String
  @Nls
  def presentableName: String
  override def hashCode(): Int = toString.hashCode
  override def equals(o: scala.Any): Boolean = toString == o.toString
}

// upstream: @PropertyMapping(Array("name", "root", "presentableName")) (com.intellij.serialization, not in Consulo)
final class SbtMavenResolver(
  override val name: String,
  override val root: String,
  @Nls _presentableName: String = null
) extends SbtResolver {

  override val presentableName: String =
    if (_presentableName != null) _presentableName else NlsString.force(name)

  override def toString = s"$root|maven|$name"

  // Replicate from MavenIndex function normalizePathOrUrl
  def normalizePathOrUrl(pathOrUrl: String): String =
    FileUtil.toSystemIndependentName(pathOrUrl.trim).reverse.dropWhile(_ == '/').reverse

  def normalizedRoot: String = normalizePathOrUrl(root)
}

// upstream: @PropertyMapping(Array("name", "root", "isLocal", "presentableName"))
final class SbtIvyResolver(
  override val name: String,
  override val root: String,
  val isLocal: Boolean,
  @Nls _presentableName: String = null
) extends SbtResolver {

  override val presentableName: String =
    if (_presentableName != null) _presentableName else NlsString.force(name)

  override def toString = s"$root|ivy|isLocal=$isLocal|$name"
}
