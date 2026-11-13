package org.jetbrains.plugins.scala.hierarchy

import consulo.language.editor.TargetElementUtil
import com.intellij.ide.hierarchy.`type`.JavaTypeHierarchyProvider
import consulo.language.editor.CommonDataKeys
import consulo.dataContext.DataContext
import consulo.codeEditor.Editor
import com.intellij.java.language.psi.PsiClass
import org.jetbrains.plugins.scala.lang.psi.api.base.Constructor
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAliasDefinition
import org.jetbrains.plugins.scala.lang.psi.types.ScTypeExt

final class ScalaTypeHierarchyProvider extends JavaTypeHierarchyProvider {
  override def getTarget(dataContext: DataContext): PsiClass = {
    val editor = Option(dataContext.getData(CommonDataKeys.EDITOR))

    editor
      .flatMap(getScalaTarget)
      .getOrElse(super.getTarget(dataContext))
  }

  private def getScalaTarget(editor: Editor): Option[PsiClass] = {
    val targetElement = TargetElementUtil.findTargetElement(editor,
      TargetElementUtil.ELEMENT_NAME_ACCEPTED |
        TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED |
        TargetElementUtil.LOOKUP_ITEM_ACCEPTED
    )

    targetElement match {
      case element: PsiClass =>
        Some(element)
      case Constructor.ofClass(clazz) =>
        Some(clazz)
      case alias: ScTypeAliasDefinition =>
        alias.aliasedType.toOption
          .flatMap(_.extractClass)
      case _ =>
        None
    }
  }
}
