package org.jetbrains.plugins.scala
package decompileToJava

import consulo.annotation.component.ExtensionImpl

import java.{util => ju}
import java.util.concurrent.CompletableFuture
import com.intellij.java.impl.codeInsight.AttachSourcesProvider
import consulo.localize.LocalizeValue
import consulo.module.content.layer.orderEntry.LibraryOrderEntry
import consulo.language.psi.PsiFile
import consulo.ui.Component
import consulo.ui.event.ComponentEvent
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile

@ExtensionImpl
class DecompileScalaToJavaActionProvider extends AttachSourcesProvider {

  import AttachSourcesProvider.AttachSourcesAction

  override def getActions(list: ju.List[LibraryOrderEntry],
                          classFile: PsiFile): ju.Collection[AttachSourcesAction] =
    classFile match {
      case file: ScFile if file.isCompiled =>
        val action = new AttachSourcesAction {
          override def getName: LocalizeValue = LocalizeValue.of(ScalaJavaDecompilerBundle.message("decompile.to.java"))

          override def getBusyText: LocalizeValue = LocalizeValue.of(ScalaJavaDecompilerBundle.message("scala.classfile"))

          override def perform(list: ju.List[LibraryOrderEntry], e: ComponentEvent[Component]): CompletableFuture[?] = {
            ScalaBytecodeDecompileTask.showDecompiledJavaCode(file)
            CompletableFuture.completedFuture(null)
          }
        }
        ju.Collections.singletonList(action)
      case _ => ju.Collections.emptyList()
    }
}
