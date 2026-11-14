package org.jetbrains.sbt.project.structure.data

import consulo.platform.Platform
import org.jetbrains.plugins.scala.extensions.PathExt

import java.net.URI
import java.nio.file.Paths
import scala.xml.Node

private object Helpers:

  extension (node: Node)
    infix def !(name: String): Node = node \ name match
      case Seq() => throw new RuntimeException("None of " + name + " nodes is found in " + node)
      case Seq(child) => child
      case _ => throw new RuntimeException("Multiple " + name + " nodes are found in " + node)

  extension (str: String)
    def uri(using platform: Platform): URI = canonUri(new URI(str.replace("\\", "/"))) // handle windows separators

  private def canonUri(uri: URI)(using platform: Platform): URI =
    val uri1 =
      if (uri.getScheme == "file") {
        if platform == Platform.current() then
          Paths.get(uri).toCanonicalPath.toUri
        else
          // upstream: eel path translation; the uri path is already local to the remote machine
          new URI("file", null, platform.fs().getPath(uri.getPath).normalize().toString.replace("\\", "/"), null)
      } else uri
    uri1.normalize()
