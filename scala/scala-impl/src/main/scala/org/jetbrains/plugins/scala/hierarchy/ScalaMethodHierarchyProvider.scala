package org.jetbrains.plugins.scala.hierarchy


import com.intellij.java.impl.ide.hierarchy.method.JavaMethodHierarchyProvider
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

class ScalaMethodHierarchyProvider extends JavaMethodHierarchyProvider {
  override def getLanguage: Language = ScalaLanguage.INSTANCE
}
