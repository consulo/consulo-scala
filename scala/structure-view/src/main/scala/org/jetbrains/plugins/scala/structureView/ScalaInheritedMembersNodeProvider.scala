package org.jetbrains.plugins.scala.structureView

import consulo.fileEditor.localize.FileEditorLocalize
import consulo.localize.LocalizeValue
import consulo.platform.base.icon.PlatformIconGroup
import com.intellij.java.impl.ide.structureView.impl.java.{JavaClassTreeElement, PsiFieldTreeElement}
import consulo.fileEditor.structureView.tree.FileStructureNodeProvider
import consulo.fileEditor.structureView.tree.{ActionPresentation, ActionPresentationData, TreeElement}
import com.intellij.java.language.JavaLanguage
import consulo.ui.ex.action.Shortcut
import consulo.ui.ex.keymap.KeymapManager
import consulo.application.dumb.IndexNotReadyException
import com.intellij.java.language.psi.util.PsiUtil
import consulo.language.psi.PsiElement
import com.intellij.java.language.psi.{PsiField, PsiMethod}
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScTypeAlias, ScValueOrVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScTemplateDefinition, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.types.PhysicalMethodSignature
import org.jetbrains.plugins.scala.structureView.element.Element

import java.util
import java.util.Collections
import scala.jdk.CollectionConverters.*

class ScalaInheritedMembersNodeProvider extends FileStructureNodeProvider[TreeElement] {

  override def provideNodes(node: TreeElement): util.Collection[TreeElement] = node match {
    case e: Element => nodesOf(e.element)
    case _ => Collections.emptyList[TreeElement]
  }

  private def nodesOf(element: PsiElement): util.Collection[TreeElement] = {
    element match {
      case clazz: ScTemplateDefinition =>
        val children = new util.LinkedHashSet[TreeElement]()
        try {
          if (!clazz.isValid) return children
          for (sign <- clazz.allSignatures if !sign.isPrivate) {
            sign match {
              case sign: PhysicalMethodSignature =>
                sign.method match {
                  case x if x.name == "$tag" || x.name == "$init$" =>
                  case x if x.containingClass == clazz =>
                  case x: ScFunction => children.addAll(Element.forPsi(x, inherited = true).asJava)
                  case x: PsiMethod => children.add(new PsiMethodTreeElementDecorator(x, true))
                }
              case _ =>
                sign.namedElement match {
                  case parameter: ScClassParameter if parameter.isClassMember && parameter.containingClass != clazz && !sign.name.endsWith("_=") =>
                    children.addAll(Element.forPsi(parameter, inherited = true).asJava)
                  case named: ScNamedElement => named.nameContext match {
                    case variable: ScValueOrVariable if variable.containingClass != clazz =>
                      children.addAll(Element.forPsi(variable, inherited = true).asJava)
                    case _ =>
                  }
                  case field: PsiField if field.containingClass != clazz =>
                    children.add(new PsiFieldTreeElement(field, true))
                  case _ =>
                }
            }
          }

          clazz.allTypeSignatures.withFilter(!_.isPrivate).map(_.namedElement).foreach {
            case alias: ScTypeAlias if alias.containingClass != clazz =>
              children.addAll(Element.forPsi(alias, inherited = true).asJava)
            case _ =>
          }

          val innerClasses = clazz.getAllInnerClasses
          innerClasses.foreach {
            case td: ScTypeDefinition if !td.getModifierList.accessModifier.exists(_.isUnqualifiedPrivateOrThis) && td.containingClass != clazz =>
              children.addAll(Element.forPsi(td, inherited = true).asJava)
            case psiClass if psiClass.getLanguage.is(JavaLanguage.INSTANCE) && PsiUtil.isAccessible(psiClass, clazz, null) && psiClass.containingClass != clazz =>
              children.add(new JavaClassTreeElement(psiClass, true))
            case _ =>
          }

          children
        }
        catch {
          case _: IndexNotReadyException => Collections.emptyList[TreeElement]
        }
      case _ => Collections.emptyList[TreeElement]
    }
  }

  override def getCheckBoxText: String = FileEditorLocalize.fileStructureToggleShowInherited().get

  override def getShortcut: Array[Shortcut] = KeymapManager.getInstance.getActiveKeymap.getShortcuts("FileStructurePopup")

  override def getPresentation: ActionPresentation = new ActionPresentationData(
    FileEditorLocalize.actionStructureviewShowInherited(), LocalizeValue.empty(), PlatformIconGroup.hierarchySupertypes())

  override def getName: String = ScalaInheritedMembersNodeProvider.ID
}

object ScalaInheritedMembersNodeProvider {
  val ID = "SCALA_SHOW_INHERITED"
}
