package org.jetbrains.plugins.scala.project.external

import java.nio.file.Path

// upstream: constructors are annotated with com.intellij.serialization.PropertyMapping (external system data serialization)
sealed abstract class SdkReference

final case class JdkByName(name: String) extends SdkReference
final case class JdkByHome(home: Path) extends SdkReference
final case class JdkByVersion(version: String) extends SdkReference
final case class AndroidJdk(version: String) extends SdkReference
