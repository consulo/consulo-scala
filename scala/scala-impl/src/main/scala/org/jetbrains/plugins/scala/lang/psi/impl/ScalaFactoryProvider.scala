package org.jetbrains.plugins.scala.lang.psi.impl

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.project.Project
import com.intellij.java.language.psi.JVMElementFactoryProvider

@ExtensionImpl
final class ScalaFactoryProvider extends JVMElementFactoryProvider {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def getFactory(project: Project): org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactoryImpl = new ScalaPsiElementFactoryImpl(project)
}
