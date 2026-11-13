package org.jetbrains.plugins.scala.actions

import consulo.annotation.component.ActionImpl

import consulo.language.editor.PlatformDataKeys
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import consulo.ui.ex.action.{AnAction, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.ui.ex.popup.BaseListPopupStep
import consulo.ui.ex.popup.{JBPopup, JBPopupFactory, PopupStep}
import consulo.ui.ex.awt.ScrollPaneFactory
import consulo.disposer.Disposer
import consulo.language.editor.util.PsiUtilBase
import consulo.language.psi.{NavigatablePsiElement, PsiDocumentManager, PsiElement, PsiNamedElement}
import consulo.ui.ex.RelativePoint
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScMethodCall, ScReferenceExpression}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

import java.awt.Point
import java.awt.event.{KeyAdapter, KeyEvent, MouseAdapter, MouseEvent}
import javax.swing.JList

@ActionImpl(id = "Scala.MakeExplicit")
final class MakeExplicitAction extends AnAction(
  ScalaBundle.message("make.implicit.conversion.explicit.action.text"),
  ScalaBundle.message("make.implicit.conversion.explicit.action.description"),
  /* icon = */ null
) {

  override def actionPerformed(event: AnActionEvent): Unit = {
    val context = event.getDataContext
    val project = context.getData(CommonDataKeys.PROJECT) match {
      case null => return
      case value => value
    }

    context.getData(PlatformDataKeys.SELECTED_ITEM) match {
      case Parameters(function: ScFunction, oldExpression, _, editor, elements) if
      oldExpression != null &&
        editor != null &&
        elements != null =>
        PsiUtilBase.getPsiFileInEditor(editor, project) match {
          case _: ScalaFile => MakeExplicitAction.showMakeExplicitPopup(oldExpression, function, elements)(using project, editor)
          case _ =>
        }
      case _ =>
    }
  }
}

object MakeExplicitAction {

  import JBPopupFactory.getInstance as PopupFactory
  import ScalaPsiElementFactory.{createExpressionFromText, createReferenceFromText}

  private val MakeExplicit = ScalaBundle.message("make.explicit")
  private val MakeExplicitStatically = ScalaBundle.message("make.explicit.and.import.method")

  private var popup: JBPopup = scala.compiletime.uninitialized

  def createPopup(list: JList[Parameters]): JBPopup = {
    GoToImplicitConversionAction.setList(list)
    popup = createPopupBuilder(list).createPopup
    popup
  }

  // upstream: com.intellij.openapi.ui.popup.PopupChooserBuilder(list) (impl class in Consulo)
  private def createPopupBuilder(list: JList[Parameters]) = {
    def onItemChosen(): Unit = list.getSelectedValue match {
        case Parameters(navigable: NavigatablePsiElement, _, _, _, _) =>
          val maybeSynthetic = navigable match {
            case function: ScFunction =>
              function.syntheticNavigationElement match {
                case synthetic: NavigatablePsiElement => Some(synthetic)
                case _ => None
              }
            case _ => None
          }

          maybeSynthetic.getOrElse(navigable).navigate(true)
        case _ =>
      }

    list.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit =
        if (e.getKeyCode == KeyEvent.VK_ENTER && !e.isAltDown) {
          e.consume()
          if (popup != null) popup.cancel()
          onItemChosen()
        }
    })
    list.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit =
        if (e.getButton == MouseEvent.BUTTON1 && e.getClickCount == 2) {
          if (popup != null) popup.cancel()
          onItemChosen()
        }
    })

    JBPopupFactory.getInstance.createComponentPopupBuilder(ScrollPaneFactory.createScrollPane(list), list)
      .setTitle(ScalaBundle.message("title.choose.implicit.conversion.method"))
      .setAdText(ScalaBundle.message("press.alt.enter"))
      .setMovable(false)
      .setResizable(false)
      .setRequestFocus(true)
  }

  def showMakeExplicitPopup(expression: ScExpression, function: ScFunction,
                            elements: Seq[PsiNamedElement])
                           (implicit project: Project, editor: Editor): Unit = {
    val step = new ActionPopupStep(expression, function, elements.contains(function))
    val list = GoToImplicitConversionAction.getList

    PopupFactory.createListPopup(step)
      .show(new RelativePoint(list, currentItemPoint(list)))
  }

  def currentItemPoint(list: JList[?], moveLeft: Int = 20): Point = list.getSelectedIndex match {
    case -1 => throw new RuntimeException("Index = -1 is less than zero.")
    case index =>
      list.getCellBounds(index, index) match {
        case null => throw new RuntimeException(s"No bounds for index = $index.")
        case bounds => new Point(bounds.x + bounds.width - moveLeft, bounds.y)
      }
  }

  private class ActionPopupStep(expression: ScExpression, function: ScFunction,
                                importStatically: Boolean)
                               (implicit project: Project, editor: Editor)
    extends BaseListPopupStep[String](null,
      (if (importStatically) Array(MakeExplicit, MakeExplicitStatically) else Array(MakeExplicit))*) {

    override def getTextFor(value: String): String = value

    override def onChosen(selectedValue: String, finalChoice: Boolean): PopupStep[?] =
      selectedValue match {
        case null =>
          PopupStep.FINAL_CHOICE
        case value if finalChoice =>
          PsiDocumentManager.getInstance(project).commitAllDocuments()
          Disposer.dispose(popup)

          value match {
            case MakeExplicit => replaceWithExplicit(expression, function, importStatically)(using project, editor)
            case MakeExplicitStatically => replaceWithExplicitStatically()
          }

          PopupStep.FINAL_CHOICE
        case _ =>
          super.onChosen(selectedValue, finalChoice)
      }

    private def replaceWithExplicitStatically(): Unit = {
      val replacementText = methodCallText(expression, function)
      val (maybeClass, prefix) = classAndPrefix(function, importStatically)(_.qualifiedName)

      runReplace(expression, replacementText)(Option(createReferenceFromText(prefix + replacementText).resolve())) {
        case (reference: ScReferenceExpression, target) => reference.bindToElement(target, maybeClass)
      }
    }
  }

  def replaceWithExplicit(expression: ScExpression, function: ScFunction,
                          importStatically: Boolean)
                         (implicit project: Project, editor: Editor): Unit = {
    val (maybeClass, prefix) = classAndPrefix(function, importStatically)(_.name)

    runReplace(expression, prefix + methodCallText(expression, function))(maybeClass) {
      case (ScReferenceExpression.withQualifier(reference: ScReferenceExpression), clazz) => reference.bindToElement(clazz)
    }
  }

  private def methodCallText(expression: ScExpression, function: ScFunction) =
    s"${function.name}(${expression.getText})"

  private def classAndPrefix(function: ScFunction, importStatically: Boolean)
                                  (className: ScTemplateDefinition => String) = {
    val maybeClass = if (importStatically) Option(function.containingClass) else None
    (maybeClass, maybeClass.fold("")(className.andThen(_ + ".")))
  }

  private def runReplace(expression: ScExpression, replacementText: String)
                              (findTarget: => Option[PsiElement])
                              (onExpression: PartialFunction[(ScExpression, PsiElement), Unit])
                              (implicit project: Project, editor: Editor): Unit = {
    def doReplace(): Unit = {
      val replacement = createExpressionFromText(replacementText, expression)

      IntentionPreviewUtils.write { () =>
        val methodCall = expression.replace(replacement).asInstanceOf[ScMethodCall]
        for {
          target <- findTarget
        } onExpression(methodCall.deepestInvokedExpr, target)

        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument)
      }
    }

    if (IntentionPreviewUtils.isIntentionPreviewActive) doReplace()
    else startCommand() { doReplace() }
  }
}
