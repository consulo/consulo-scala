package org.jetbrains.plugins.scala.projectView

import consulo.ui.ex.tree.PresentationData
import consulo.project.ui.view.tree.ViewSettings
import consulo.project.Project
import consulo.util.io.FileUtil.getNameWithoutExtension
import consulo.ui.ex.SimpleTextAttributes
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

private[projectView] class PackageObjectNode(definition: ScTypeDefinition)
                                            (implicit project: Project, settings: ViewSettings)
  extends TypeDefinitionNode(definition) {

  override def getTypeSortWeight(sortByType: Boolean): Int =
    if (groupWithPackage) 2 else 4 // PsiDirectoryNode.getTypeSortWeight == 3

  private def groupWithPackage: Boolean =
    ScalaProjectSettings.getInstance(myProject).isGroupPackageObjectWithPackage

  override def update(data: PresentationData): Unit = {
    super.update(data)

    if (!groupWithPackage && definition.isValid) {
      data.clearText()
      Option(definition.getContainingFile).foreach { file =>
        //noinspection ScalaExtractStringToBundle,ReferencePassedToNls
        data.addText(getNameWithoutExtension(file.name) + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
      }
    }
  }
}
