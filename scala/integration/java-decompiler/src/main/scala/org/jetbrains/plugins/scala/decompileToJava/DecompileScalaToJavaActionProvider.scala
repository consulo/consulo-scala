package org.jetbrains.plugins.scala
package decompileToJava

import consulo.annotation.component.ExtensionImpl

import java.{util => ju}
import com.intellij.codeInsight.AttachSourcesProvider
import consulo.module.content.layer.orderEntry.LibraryOrderEntry
import com.intellij.openapi.util.ActionCallback
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile

@ExtensionImpl
class DecompileScalaToJavaActionProvider extends AttachSourcesProvider {

  import AttachSourcesProvider.AttachSourcesAction

  override def getActions(list: ju.List[? <: LibraryOrderEntry],
                          classFile: PsiFile): ju.Collection[AttachSourcesAction] =
    classFile match {
      case file: ScFile if file.isCompiled =>
        val action = new AttachSourcesAction {
          override def getName: String = ScalaJavaDecompilerBundle.message("decompile.to.java")

          override def getBusyText: String = ScalaJavaDecompilerBundle.message("scala.classfile")

          override def perform(list: ju.List[? <: LibraryOrderEntry]): ActionCallback = {
            ScalaBytecodeDecompileTask.showDecompiledJavaCode(file)
            ActionCallback.DONE
          }
        }
        ju.Collections.singletonList(action)
      case _ => ju.Collections.emptyList()
    }
}
