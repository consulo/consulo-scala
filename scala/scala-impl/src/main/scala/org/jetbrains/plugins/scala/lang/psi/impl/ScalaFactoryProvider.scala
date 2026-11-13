package org.jetbrains.plugins.scala.lang.psi.impl

import consulo.project.Project
import com.intellij.java.language.psi.JVMElementFactoryProvider

final class ScalaFactoryProvider extends JVMElementFactoryProvider {

  override def getFactory(project: Project): org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactoryImpl = new ScalaPsiElementFactoryImpl(project)
}
