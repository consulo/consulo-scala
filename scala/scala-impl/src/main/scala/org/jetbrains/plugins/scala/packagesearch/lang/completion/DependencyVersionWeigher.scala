package org.jetbrains.plugins.scala.packagesearch.lang.completion

import consulo.language.util.NegatingComparable
import consulo.language.editor.completion.lookup.{LookupElement, LookupElementWeigher}
import org.apache.maven.artifact.versioning.ComparableVersion

object DependencyVersionWeigher extends LookupElementWeigher("scalaDependencyVersionWeigher") {
  override def weigh(element: LookupElement): Comparable[?] = {
    val version = element.getObject match {
      case DependencyVersion(version) => version
      case _ => new ComparableVersion("0.0.0")
    }
    new NegatingComparable(version)
  }

  final case class DependencyVersion(version: ComparableVersion)
}
