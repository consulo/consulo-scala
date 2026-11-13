package org.jetbrains.plugins.scala.codeInspection.deprecation

import org.jetbrains.plugins.scala.codeInspection.{PsiUpdateContext, PsiUpdateFix}
import consulo.language.editor.inspection.{LocalInspectionTool, LocalQuickFix, ProblemsHolder}
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import org.jetbrains.plugins.scala.codeInsight.unwrap.{ScalaUnwrapContext, ScalaUnwrapper}
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt

class Scala3DeprecatedPackageObjectInspection extends LocalInspectionTool {

  import Scala3DeprecatedPackageObjectInspection._

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case obj: ScObject if obj.isPackageObject && obj.isInScala3Module =>
      val fix = unwrapPackageObjectQuickFix(obj).toSeq
      holder.registerProblem(obj.nameId, message, fix*)
    case _ =>
  }
}

object Scala3DeprecatedPackageObjectInspection {
  private[deprecation] val message = ScalaInspectionBundle.message("package.objects.are.deprecated")
  private[deprecation] val fixId = ScalaInspectionBundle.message("unwrap.package.object.fix")
  private val unwrapper = new PackageObjectUnwrapper

  private def unwrapPackageObjectQuickFix(obj: ScObject): Option[PsiUpdateFix[?]] =
    Option.when(obj.extendsBlock.templateParents.forall(_.typeElements.isEmpty))(
      new PsiUpdateFix[ScObject](obj) {
        override def getFamilyName: String = fixId

        override protected def invoke(context: PsiUpdateContext, obj: ScObject): Unit =
          unwrapper.unwrap(obj)
      }
    )

  private class PackageObjectUnwrapper extends ScalaUnwrapper {
    override def isApplicableTo(e: PsiElement): Boolean = true

    override def doUnwrap(element: PsiElement, context: ScalaUnwrapContext): Unit = {
      context.extractAllMembers(element.asInstanceOf[ScObject])
      context.deleteExactly(element)
    }

    def unwrap(obj: ScObject): Unit = {
      val context = createContext()
      context.setIsEffective(true)
      doUnwrap(obj, context)
    }
  }
}
