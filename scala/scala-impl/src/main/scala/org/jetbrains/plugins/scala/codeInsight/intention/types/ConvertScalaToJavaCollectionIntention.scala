package org.jetbrains.plugins.scala.codeInsight.intention.types

import consulo.localize.LocalizeValue

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview
import consulo.project.Project
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.scope.GlobalSearchScope
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager

/**
 * Converts expression representing scala collection to
 * java equivalent using [[scala.collection.JavaConverters]] before Scala 2.13
 * and [[scala.jdk.CollectionConverters]] since Scala 2.13
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ConvertScalaToJavaCollectionIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Type"))
class ConvertScalaToJavaCollectionIntention extends BaseJavaConvertersIntention("asJava") {

  override def targetCollections(project: Project, scope: GlobalSearchScope): Set[PsiClass] = {
    val manager = ScalaPsiManager.instance(project)
    Set(
      "scala.collection.Seq",
      "scala.collection.Set",
      "scala.collection.Map",
      "scala.collection.Iterator",
      "scala.collection.Iterable"
    ).flatMap(fqn => manager.getCachedClass(scope, fqn))
  }

  @SafeFieldForPreview
  override val alreadyConvertedPrefixes: Set[String] = Set("java.")

  override def getText: LocalizeValue = LocalizeValue.of(ScalaBundle.message("convert.scala.to.java.collection.hint"))

  def getFamilyName: String = ConvertScalaToJavaCollectionIntention.getFamilyName
}

object ConvertScalaToJavaCollectionIntention {
  def getFamilyName: String = ScalaBundle.message("convert.scala.to.java.collection.name")
}
