package org.jetbrains.plugins.scala.codeInspection.varCouldBeValInspection

import consulo.codeEditor.CodeInsightColors
import consulo.colorScheme.TextAttributesKey
import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.localize.LocalizeValue
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.annotator.usageTracker.ScalaRefCountHolder
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.{HighlightingPassInspection, ProblemInfo}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.{isOnlyVisibleInLocalFile, isPossiblyAssignment}
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariableDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createValFromVarDefinition

import scala.jdk.CollectionConverters._

@ExtensionImpl
class VarCouldBeValInspection extends HighlightingPassInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.var.could.be.a.val"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def editorAttributes: TextAttributesKey = CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES


  import VarCouldBeValInspection._

  override def invoke(element: PsiElement, isOnTheFly: Boolean): Seq[ProblemInfo] = element match {
    case variable: ScVariableDefinition
      if variable.declaredElements.forall(if (isOnTheFly) hasNoWriteUsagesOnTheFly else hasNoWriteUsages) =>
      Seq(ProblemInfo(variable.keywordToken, ScalaInspectionBundle.message("var.could.be.a.val"), Seq(new VarToValFix(variable))))
    case _ => Seq.empty
  }

  override def shouldProcessElement(element: PsiElement): Boolean = element match {
    case variable: ScVariableDefinition =>
      isOnlyVisibleInLocalFile(variable)
    case _ => false
  }

  override def isEnabledByDefault: Boolean = true
}

object VarCouldBeValInspection {

  val ID: String = "VarCouldBeVal"

  class VarToValFix(variable: ScVariableDefinition) extends LocalQuickFixAndIntentionActionOnPsiElement(variable) {
    override def invoke(project: Project, file: PsiFile, editor: Editor, startElement: PsiElement, endElement: PsiElement): Unit =
      startElement match {
        case variable: ScVariableDefinition =>
          val file = variable.getContainingFile
          if (file != null && IntentionPreviewUtils.prepareElementForWrite(file)) {
            val replacement = createValFromVarDefinition(variable)
            variable.replace(replacement)
          }
        case _ =>
      }

    override def getText: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("convert.var.to.val"))

    def getFamilyName: String = getText.get
  }

  private def hasNoWriteUsagesOnTheFly(element: ScBindingPattern): Boolean = {
    var hasWriteUsages = false
    var used = false
    val holder = ScalaRefCountHolder(element)
    holder.runIfUnusedReferencesInfoIsAlreadyRetrievedOrSkip { () =>
      hasWriteUsages = holder.isValueWriteUsed(element)
      used = holder.isValueReadUsed(element)
    }
    !hasWriteUsages && used // has no write usages but is used
  }

  private def hasNoWriteUsages(element: ScBindingPattern): Boolean = {
    val (write, read) = findReferences(element)
    read.nonEmpty && write.isEmpty
  }

  private def findReferences(element: PsiElement): (Iterable[PsiElement], Iterable[PsiElement]) = {
    val references = ReferencesSearch.search(element, element.getUseScope)
      .findAll().asScala
      .map(_.getElement)

    references.partition(isPossiblyAssignment)
  }
}
