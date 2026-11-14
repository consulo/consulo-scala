package org.jetbrains.sbt.project.structure.data

import consulo.platform.Platform
import org.jetbrains.sbt.project.structure.data.Helpers.uri

case class ResolverData(name: String, root: String)

object ResolverData:
  given Platform => XmlDeserializer[ResolverData] = what =>
    val name = (what \ "@name").text
    val root = (what \ "@root").text
    val canonRoot = root.uri.normalize().toString
    Right(ResolverData(name, canonRoot))
