package org.jetbrains.plugins.scala.codeInspection.collections

import consulo.configurable.{ConfigurableBuilder, UnnamedConfigurable}
import consulo.language.editor.inspection.{InspectionToolState, LocalInspectionTool, LocalInspectionToolSession, ProblemHighlightType, ProblemsHolder}
import consulo.language.psi.PsiElementVisitor
import consulo.localize.LocalizeValue
import consulo.util.xml.serializer.XmlSerializerUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInspection.collections.OperationOnCollectionInspectionBase._
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.settings.{ScalaApplicationSettings, ScalaProjectSettingsUtil}

import java.{util => ju}
import scala.beans.BeanProperty
import scala.collection.immutable.ArraySeq

object OperationOnCollectionInspectionBase {
  val inspectionId: String = ScalaInspectionBundle.message("operation.on.collection.id")
  val inspectionName: String = ScalaInspectionBundle.message("operation.on.collection.name")

  val likeOptionClassesDefault: Array[String] = Array("scala.Option", "scala.Some", "scala.None")
  val likeCollectionClassesDefault: Array[String] = Array("scala.collection._", "scala.Array", "scala.Option", "scala.Some", "scala.None", "java.lang.String")

  private[collections] val likeOptionKey = "operation.on.collection.like.option"
  private[collections] val likeCollectionKey = "operation.on.collection.like.collection"

  private[collections] val inputMessages = Map(
    likeCollectionKey -> ScalaInspectionBundle.message("operation.on.collection.like.collection.input.message"),
    likeOptionKey -> ScalaInspectionBundle.message("operation.on.collection.like.option.input.message")
  )

  private[collections] val inputTitles = Map(
    likeCollectionKey -> ScalaInspectionBundle.message("operation.on.collection.like.collection.input.title"),
    likeOptionKey -> ScalaInspectionBundle.message("operation.on.collection.like.option.input.title")
  )

  private[collections] val panelTitles = Map(
    likeCollectionKey -> ScalaInspectionBundle.message("operation.on.collection.like.collection.panel.title"),
    likeOptionKey -> ScalaInspectionBundle.message("operation.on.collection.like.option.panel.title")
  )

  object SimplifiableExpression {
    def unapply(expr: ScExpression): Option[ScExpression] =
      if (expr.is[ScBlock, ScParenthesisedExpr]) None
      else Some(expr)
  }

}

abstract class OperationOnCollectionInspectionBase extends LocalInspectionTool {
  private val settings = ScalaApplicationSettings.getInstance()

  override def createStateProvider(): InspectionToolState[?] =
    new OperationOnCollectionInspectionState(possibleSimplificationTypes.map(_.description))

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession, state: Object): PsiElementVisitor =
    buildVisitor(holder, state.asInstanceOf[OperationOnCollectionInspectionState].isEnabled)

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    buildVisitor(holder, (_: Int) => true)

  private def buildVisitor(holder: ProblemsHolder, isEnabled: Int => Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case SimplifiableExpression(expr) => simplifications(expr, isEnabled).foreach {
      case s@Simplification(toReplace, _, hint, rangeInParent) =>
        val quickFix = (OperationOnCollectionQuickFix(s))
        holder.registerProblem(toReplace.getElement, hint, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, rangeInParent, quickFix)
    }
    case _ =>
  }

  private def simplifications(expr: ScExpression, isEnabled: Int => Boolean): Seq[Simplification] = {
    def simplificationTypes = for {
      (st, idx) <- possibleSimplificationTypes.zipWithIndex
      if isEnabled(idx)
    } yield st

    simplificationTypes.flatMap(st => st.getSimplifications(expr) ++ st.getSimplification(expr))
  }

  def getLikeCollectionClasses: Seq[String] = ArraySeq.unsafeWrapArray(settings.getLikeCollectionClasses)
  def getLikeOptionClasses: Seq[String] = ArraySeq.unsafeWrapArray(settings.getLikeOptionClasses)
  def setLikeCollectionClasses(values: Seq[String]): Unit = settings.setLikeCollectionClasses(values.toArray)
  def setLikeOptionClasses(values: Seq[String]): Unit = settings.setLikeOptionClasses(values.toArray)

  def possibleSimplificationTypes: Seq[SimplificationType]

}

abstract class OperationOnCollectionInspection extends OperationOnCollectionInspectionBase

/**
 * Enabled simplification types are stored per inspection, "like collection"/"like option" patterns are application-wide
 * ([[ScalaApplicationSettings]]), like upstream.
 */
final class OperationOnCollectionInspectionState(descriptions: Seq[String])
  extends InspectionToolState[OperationOnCollectionInspectionState] {

  import OperationOnCollectionInspectionBase._

  def this() = this(Seq.empty)

  @BeanProperty var simplificationTypesEnabled: Array[java.lang.Boolean] = Array.fill(descriptions.size)(java.lang.Boolean.TRUE)

  def isEnabled(idx: Int): Boolean =
    idx >= simplificationTypesEnabled.length || simplificationTypesEnabled(idx).booleanValue()

  private def setEnabled(idx: Int, enabled: Boolean): Unit = {
    if (idx >= simplificationTypesEnabled.length)
      simplificationTypesEnabled = simplificationTypesEnabled.padTo(descriptions.size, java.lang.Boolean.TRUE)
    simplificationTypesEnabled(idx) = enabled
  }

  override def createConfigurable(): UnnamedConfigurable = {
    val builder = ConfigurableBuilder.newBuilder()
    if (descriptions.sizeIs > 1) descriptions.zipWithIndex.foreach { case (description, idx) =>
      builder.checkBox(LocalizeValue.of(description), () => isEnabled(idx), setEnabled(idx, _))
    }
    patternList(builder, likeCollectionKey, _.getLikeCollectionClasses, _.setLikeCollectionClasses(_))
    patternList(builder, likeOptionKey, _.getLikeOptionClasses, _.setLikeOptionClasses(_))
    builder.buildUnnamed()
  }

  private def patternList(builder: ConfigurableBuilder[?],
                          patternListKey: String,
                          getter: ScalaApplicationSettings => Array[String],
                          setter: (ScalaApplicationSettings, Array[String]) => Unit): Unit = {
    builder.component(() => consulo.ui.Label.create(LocalizeValue.of(panelTitles(patternListKey))))
    builder.textBoxWithExpandAction(
      null,
      inputTitles(patternListKey),
      (text: String) => ju.Arrays.asList(text.split(",").map(_.trim).filter(_.nonEmpty)*),
      (values: ju.List[String]) => String.join(", ", values),
      () => getter(ScalaApplicationSettings.getInstance()).mkString(", "),
      (text: String) => setter(ScalaApplicationSettings.getInstance(), text.split(",").map(_.trim).filter(_.nonEmpty))
    )
  }

  override def getState: OperationOnCollectionInspectionState = this

  override def loadState(state: OperationOnCollectionInspectionState): Unit = XmlSerializerUtil.copyBean(state, this)
}
