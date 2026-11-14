package org.jetbrains.plugins.scala.scalai18n.codeInspection.i18n

import com.intellij.java.language.codeInsight.AnnotationUtil
import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.inspection.localize.InspectionLocalize
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import consulo.language.editor.localize.CodeInsightLocalize
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import org.jetbrains.plugins.scala.annotator.element.ScMethodInvocationAnnotator
import org.jetbrains.plugins.scala.codeInspection.PsiElementVisitorSimple
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScAnnotation, ScParenthesizedElement}
import org.jetbrains.plugins.scala.lang.psi.api.expr.*

//noinspection InstanceOf
@ExtensionImpl
class ScalaInvalidPropertyKeyInspection extends LocalInspectionTool {
  override def getShortName: String = "ScalaUnresolvedPropertyKey"

  override def getDisplayName: LocalizeValue = CodeInsightLocalize.inspectionUnresolvedPropertyKeyReferenceName()

  override def getGroupDisplayName: LocalizeValue = InspectionLocalize.groupNamesPropertiesFiles()

  override def getGroupPath: Array[LocalizeValue] = Array(LocalizeValue.of("Scala"), getGroupDisplayName)

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.ERROR

  override def isEnabledByDefault: Boolean = true

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case invoc@MethodInvocation(_, Seq(ScParenthesizedElement.InnermostElement(stringLit: ScStringLiteral), restArgs @ _*)) =>
      // check the property reference and the number of parameters passed
      val key = stringLit.getValue
      val resolver = ScalaI18nUtil.PropertyReferenceResolver(stringLit)

      if (resolver.isPassedToPropertyKey) {
        if (!resolver.referenceIsValid) {
          val description = CodeInsightLocalize.inspectionUnresolvedPropertyKeyReferenceMessage(key).get
          holder.registerProblem(stringLit, description, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
        } else {
          // check if the correct number of parameters was passed
          lazy val possibleParamCounts = resolver.possibleParamCounts
          val msgArgCount = restArgs.length
          def lastArgIsSeqArg = restArgs.lastOption.flatMap(_.asOptionOf[ScTypedExpression]).exists(_.isSequenceArg)

          // don't register a problem if there was already a problem with calling the method
          val methodCalledCorrectly = invoc.target.exists(_.problems.isEmpty)

          if (methodCalledCorrectly && !possibleParamCounts.contains(msgArgCount)) {
            val msgParamCount = possibleParamCounts.max
            val description = CodeInsightLocalize.propertyHasMoreParametersThanPassed(key, msgParamCount, msgArgCount).get
            if (msgArgCount > msgParamCount) {
              for (arg <- restArgs.iterator.drop(msgParamCount)) {
                holder.registerProblem(arg, description, ProblemHighlightType.GENERIC_ERROR)
              }
            } else if (msgArgCount < msgParamCount && !lastArgIsSeqArg) {
              val range = ScMethodInvocationAnnotator.missingArgumentsRange(invoc)
                .shiftLeft(invoc.getTextRange.getStartOffset)
              holder.registerProblem(invoc, description, ProblemHighlightType.GENERIC_ERROR, range)
            }
          }
        }
      }
    case annotation: ScAnnotation if annotation.getQualifiedName == AnnotationUtil.PROPERTY_KEY =>
      // check whether a bundle reference is invalid
      annotation.findAttributeValue(AnnotationUtil.PROPERTY_KEY_RESOURCE_BUNDLE_PARAMETER) match {
        case bundleNameElement: PsiElement =>
          val bundleName = ScalaI18nUtil.resolveBundleName(bundleNameElement)
          val isValidBundle = bundleName.exists { bundleName =>
            ScalaI18nUtil.propertiesFilesByBundleName(bundleName, bundleNameElement).nonEmpty
          }

          if (!isValidBundle) {
            val description = CodeInsightLocalize.inspectionInvalidResourceBundleReference(bundleName.getOrElse(bundleNameElement.getText)).get
            holder.registerProblem(bundleNameElement, description, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
          }
        case null =>
      }
    case _ =>
  }
}
