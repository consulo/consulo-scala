package org.jetbrains.plugins.scala.lang.refactoring.introduceParameter

import consulo.document.RangeMarker
import consulo.project.Project
import consulo.util.lang.ref.Ref
import consulo.document.util.TextRange
import consulo.language.psi.{PsiDocumentManager, PsiElement, PsiFile}
import consulo.language.editor.refactoring.changeSignature.{ChangeInfo, ChangeSignatureUsageProcessor}
import consulo.language.editor.refactoring.ResolveSnapshotProvider
import consulo.language.editor.refactoring.ResolveSnapshotProvider.ResolveSnapshot
import consulo.usage.UsageInfo
import consulo.util.collection.MultiMap
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaRefactoringUtil

import java.util
import scala.collection.immutable.ArraySeq

class ScalaIntroduceParameterUsageProcessor extends ChangeSignatureUsageProcessor {

  override def findUsages(info: ChangeInfo): Array[UsageInfo] = info match {
    case isIntroduceParameter(data) =>
      def toUsageInfo(range: TextRange) = {
        val file = data.methodToSearchFor.getContainingFile
        val doc = PsiDocumentManager.getInstance(data.getProject).getDocument(file)
        TextRangeUsageInfo(file, doc.createRangeMarker(range))
      }

      val occurrences = if (data.replaceAll) data.occurrences else Seq(data.mainOcc)
      occurrences.map(toUsageInfo).toArray
    case _ => Array.empty
  }

  override def processUsage(changeInfo: ChangeInfo, usageInfo: UsageInfo, beforeMethodChange: Boolean, usages: Array[UsageInfo]): Boolean = {
    if (!beforeMethodChange) return false

    changeInfo match {
      case isIntroduceParameter(data) =>
        val textRangeUsages = usages.iterator
          .collect { case t: TextRangeUsageInfo => t }
          .to(ArraySeq)
        if (textRangeUsages.headOption.forall(_.processed)) return false

        val pName = data.paramName
        val args = data.functionalArgParams.getOrElse("")
        val text = s"$pName$args"
        val file = textRangeUsages.head.file

        val manager = PsiDocumentManager.getInstance(file.getProject)
        manager.doPostponedOperationsAndUnblockDocument(manager.getDocument(file))

        ScalaRefactoringUtil.replaceOccurrences(textRangeUsages.map(usage => TextRange.create(usage.range)), text, file)
        textRangeUsages.foreach(_.processed = true)
        true
      case _ => false
    }
  }

  override def processPrimaryMethod(changeInfo: ChangeInfo): Boolean = false

  override def shouldPreviewUsages(changeInfo: ChangeInfo, usages: Array[UsageInfo]): Boolean = false

  override def findConflicts(info: ChangeInfo, refUsages: Ref[Array[UsageInfo]]): MultiMap[PsiElement, String] = new MultiMap[PsiElement, String]()

  override def registerConflictResolvers(snapshots: util.List[? >: ResolveSnapshot],
                                         resolveSnapshotProvider: ResolveSnapshotProvider,
                                         usages: Array[UsageInfo],
                                         changeInfo: ChangeInfo): Unit = {}

  override def setupDefaultValues(changeInfo: ChangeInfo, refUsages: Ref[Array[UsageInfo]], project: Project): Boolean = true

}

private case class TextRangeUsageInfo(file: PsiFile, range: RangeMarker) extends UsageInfo(file, range.getStartOffset, range.getEndOffset) {
  var processed = false
}