package org.jetbrains.plugins.scala.lang.psi.compiled

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.debugger.{DebuggerManager, NameMapper}
import consulo.application.ApplicationManager
import consulo.project.Project
import consulo.project.startup.PostStartupActivity
import consulo.application.dumb.DumbAware
import consulo.ui.UIAccess
import com.intellij.java.language.psi.PsiClass
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTemplateDefinition}
import org.jetbrains.plugins.scala.lang.psi.compiled.ScalaCompilerLoader._
import org.jetbrains.plugins.scala.project.ProjectExt

@ExtensionImpl
class ScalaCompilerLoader extends PostStartupActivity with DumbAware {
  override def runActivity(project: Project, uiAccess: UIAccess): Unit = {
    if (!isDisabled) {
      val mapper = new ScalaNameMapper()
      DebuggerManager.getInstance(project).addClassNameMapper(mapper)

      invokeOnDispose(project.unloadAwareDisposable) {
        DebuggerManager.getInstance(project).removeClassNameMapper(mapper)
      }
    }
  }
}

object ScalaCompilerLoader {
  private[compiled] def isDisabled: Boolean = {
    val application = ApplicationManager.getApplication
    !application.isUnitTestMode &&
      // The following check is hardly bulletproof, however (currently) there is no API to query that
      application.getClass.getSimpleName.contains("Upsource")
  }

  private class ScalaNameMapper extends NameMapper {
    override def getQualifiedName(clazz: PsiClass): String = clazz match {
      case tmpl: ScTemplateDefinition => inReadAction {
        javaName(tmpl)
      }
      case _ => null
    }
  }

  private def javaName(clazz: ScTemplateDefinition): String = {
    clazz.qualifiedName + (if (clazz.isInstanceOf[ScObject]) "$" else "")
  }
}
