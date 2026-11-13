package org.jetbrains.plugins.scala.overrideImplement

import com.intellij.java.language.psi.PsiClass
import consulo.language.editor.generation.{MemberChooserBuilder, ClassMember => JClassMember}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.util.dataholder.KeyWithDefaultValue
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiNamedElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScExtension
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object ScalaMemberChooser {
  private val AddOverrideModifierKey: KeyWithDefaultValue[java.lang.Boolean] =
    KeyWithDefaultValue.create("scala.member.chooser.add.override", () => java.lang.Boolean.valueOf(ScalaApplicationSettings.getInstance.ADD_OVERRIDE_TO_IMPLEMENTED))
  private val CopyScalaDocKey: KeyWithDefaultValue[java.lang.Boolean] =
    KeyWithDefaultValue.create("scala.member.chooser.copy.scaladoc", () => java.lang.Boolean.valueOf(ScalaApplicationSettings.getInstance.COPY_SCALADOC))
  private val SpecifyTypeKey: KeyWithDefaultValue[java.lang.Boolean] =
    KeyWithDefaultValue.create("scala.member.chooser.specify.type", () => java.lang.Boolean.valueOf(
      ScalaApplicationSettings.getInstance.SPECIFY_RETURN_TYPE_EXPLICITLY == ScalaApplicationSettings.ReturnTypeLevel.ADD))

  /**
   * upstream: `ScalaMemberChooser` was a `com.intellij.ide.util.MemberChooser` dialog (impl class in Consulo).
   * Not ported: preselection of the elements, the three-state "specify return type" checkbox
   * (unchecked means "by code style") and the link to the type annotation settings.
   */
  def show[T <: ClassMember](
    elements: Seq[T],
    allowEmptySelection: Boolean,
    allowMultiSelection: Boolean,
    needAddOverrideChb: Boolean,
    needSpecifyTypeChb: Boolean,
    needCopyScalaDocChb: Boolean,
    targetClass: ScTemplateDefinition,
    title: String
  )(onSelected: Seq[T] => Unit)(implicit project: Project): Unit = {
    var builder = MemberChooserBuilder.create[JClassMember](sorted(elements, targetClass).toArray[JClassMember])
      .withTitle(LocalizeValue.of(title))
    if (allowEmptySelection) builder = builder.withEmptySelection()
    if (allowMultiSelection) builder = builder.withMultipleSelection()
    if (needCopyScalaDocChb) builder = builder.withOption(CopyScalaDocKey, LocalizeValue.of(ScalaBundle.message("copy.scaladoc")))
    if (needAddOverrideChb) builder = builder.withOption(AddOverrideModifierKey, LocalizeValue.of(ScalaBundle.message("add.override.modifier")))
    if (needSpecifyTypeChb) builder = builder.withOption(SpecifyTypeKey, LocalizeValue.of(ScalaBundle.message("specify.return.type.explicitly")))

    builder.showAsync(project, result => {
      val settings = ScalaApplicationSettings.getInstance
      if (needAddOverrideChb)
        settings.ADD_OVERRIDE_TO_IMPLEMENTED = AddOverrideModifierKey.get(result)
      if (needCopyScalaDocChb)
        settings.COPY_SCALADOC = CopyScalaDocKey.get(result)
      if (needSpecifyTypeChb)
        settings.SPECIFY_RETURN_TYPE_EXPLICITLY =
          if (SpecifyTypeKey.get(result)) ScalaApplicationSettings.ReturnTypeLevel.ADD
          else ScalaApplicationSettings.ReturnTypeLevel.BY_CODE_STYLE

      val selected = Option(result.getUserData(JClassMember.KEY_OF_LIST))
        .map(_.asScala.toSeq.map(_.asInstanceOf[T]))
        .getOrElse(Seq.empty)
      onSelected(selected)
    })
  }

  def sorted[T <: ClassMember](members: Seq[T], targetClass: ScTemplateDefinition): Seq[T] = {
    val result = sortedImpl(members, targetClass, groupExtensionMethods = false)
    //We are sure that original members instances won't be modified, just sorted
    result.asInstanceOf[Seq[T]]
  }

  def sortedWithExtensionMethodsGrouped[T <: ClassMember](members: Seq[T], targetClass: ScTemplateDefinition): Seq[ClassMember0] =
    sortedImpl(members, targetClass, groupExtensionMethods = true)

  private def sortedImpl[T <: ClassMember](
    members: Seq[T],
    targetClass: ScTemplateDefinition,
    groupExtensionMethods: Boolean
  ): Seq[ClassMember0] = {
    val clazzToMembers: Map[PsiClass, Seq[T]] =
      members.groupBy(cm => cm.getElement.getContainingClass)

    val sortedClasses: mutable.Set[PsiClass] =
      mutable.LinkedHashSet()

    if (targetClass != null) {
      val supers = targetClass.supers
      sortedClasses ++= supers
    }

    val ordering = new Ordering[PsiClass] {
      override def compare(c1: PsiClass, c2: PsiClass): Int = {
        val less = c1.isInheritor(c2, /*checkDeep =*/ true)
        val more = c2.isInheritor(c1, /*checkDeep =*/ true)
        if (less && more) 0 //it is possible to have cyclic inheritance for generic traits in scala
        else if (less) -1
        else if (more) 1
        else c1.name.compareTo(c2.name)
      }
    }
    val groupedMembersSorted = clazzToMembers.keys.toSeq.sorted(using ordering)
    sortedClasses ++= groupedMembersSorted

    sortedClasses.flatMap { c =>
      val membersFromClass: Seq[T] = clazzToMembers.getOrElse(c, Seq.empty)
      val membersFromClassMaybeGrouped =
        if (groupExtensionMethods)
          doGroupExtensionMethods(membersFromClass)
        else
          membersFromClass

      membersFromClassMaybeGrouped.sortBy(_.getElement.getTextOffset)
    }.toSeq
  }

  /**
   * Replace all instances of `ScExtensionMethodMember` belonging to the same extension
   * with an instance of `ScExtensionMember`, which groups all extension methods
   */
  private def doGroupExtensionMethods[T <: ClassMember](members: Seq[T]): Seq[ClassMember0] = {
    val extensionsGrouped: Map[Option[ScExtension], Seq[T]] =
      members.groupBy { m =>
        val extensionMethod = m.asOptionOfUnsafe[ScExtensionMethodMember]
        val extension = extensionMethod.flatMap(_.signature.extensionSignature.map(_.extension))
        extension
      }

    extensionsGrouped.toSeq.flatMap {
      case (None, nonExtensions) =>
        nonExtensions
      case (Some(extension), members) =>
        val extensionMethodsSorted = members.asInstanceOf[Seq[ScExtensionMethodMember]].sortBy(_.getElement.getTextOffset)
        Seq(ScExtensionMember(extension, extensionMethodsSorted))
    }
  }
}
