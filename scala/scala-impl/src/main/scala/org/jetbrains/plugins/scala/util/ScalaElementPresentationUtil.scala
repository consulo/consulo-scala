package org.jetbrains.plugins.scala.util

import consulo.platform.base.icon.PlatformIconGroup

import consulo.ui.image.Image
import com.intellij.java.language.codeInsight.TestFrameworks
import consulo.project.DumbService
import consulo.component.util.Iconable
import consulo.language.icon.IconDescriptor
import com.intellij.java.language.psi.util.InheritanceUtil
import com.intellij.java.language.psi.{CommonClassNames, JavaPsiFacade, PsiModifier, PsiModifierListOwner}
import org.jetbrains.plugins.scala.caches.{BlockModificationTracker, cachedInUserData}
import org.jetbrains.plugins.scala.extensions.PsiModifierListOwnerExt
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition


/**
 * See also [[com.intellij.psi.impl.ElementPresentationUtil]]
 *
 * This class mirrors some private constants form [[com.intellij.psi.impl.ElementPresentationUtil]].
 * We can get rid of it once the constants become public
 */
//noinspection ApiStatus,UnstableApiUsage
object ScalaElementPresentationUtil {
  private val FLAGS_ABSTRACT = 0x100
  private val FLAGS_FINAL = 0x400
  private val FLAGS_JUNIT_TEST = 0x2000
  private val FLAGS_RUNNABLE = 0x4000
  private val FLAGS_LOCKED = 0x800 // upstream: com.intellij.psi.impl.ElementBase.FLAGS_LOCKED

  private lazy val ExceptionIcon = PlatformIconGroup.nodesExceptionclass()
  private lazy val AbstractExceptionIcon = PlatformIconGroup.nodesAbstractexception()

  // upstream: IconManager.createLayeredIcon + ElementPresentationUtil.addVisibilityIcon (no layered icon flags in Consulo)
  def getIconWithLayeredFlags(element: PsiModifierListOwner, flags: Int, icon: Image, layerFlags: Int): Image = {
    val descriptor = new IconDescriptor(icon)
    if ((layerFlags & FLAGS_FINAL) != 0) descriptor.addLayerIcon(PlatformIconGroup.nodesFinalmark())
    if ((layerFlags & FLAGS_LOCKED) != 0) descriptor.addLayerIcon(PlatformIconGroup.nodesLocked())
    if ((layerFlags & FLAGS_RUNNABLE) != 0) descriptor.addLayerIcon(PlatformIconGroup.nodesRunnablemark())
    if ((layerFlags & FLAGS_JUNIT_TEST) != 0) descriptor.addLayerIcon(PlatformIconGroup.nodesJunittestmark())

    if ((flags & Iconable.ICON_FLAG_VISIBILITY) != 0) {
      if (element.hasModifierProperty(PsiModifier.PRIVATE)) descriptor.setRightIcon(PlatformIconGroup.nodesC_private())
      else if (element.hasModifierProperty(PsiModifier.PROTECTED)) descriptor.setRightIcon(PlatformIconGroup.nodesC_protected())
      else if (element.hasModifierProperty(PsiModifier.PACKAGE_LOCAL)) descriptor.setRightIcon(PlatformIconGroup.nodesC_plocal())
      else if (element.hasModifierProperty(PsiModifier.PUBLIC)) descriptor.setRightIcon(PlatformIconGroup.nodesC_public())
    }
    descriptor.toIcon
  }

  def getBaseLayerFlags(element: PsiModifierListOwner, flags: Int): Int = {
    val maybeFinalFlag = if (Option(element.getModifierList).exists(_.hasExplicitModifier(PsiModifier.FINAL))) FLAGS_FINAL else 0
    val maybeAbstractFlag = if (element.hasAbstractModifier) FLAGS_ABSTRACT else 0
    val isLocked = (flags & Iconable.ICON_FLAG_READ_STATUS) != 0 && !element.isWritable
    val maybeLockedFlag = if (isLocked) FLAGS_LOCKED else 0

    maybeFinalFlag | maybeAbstractFlag | maybeLockedFlag
  }

  /**
   * ATTENTION: this method involves traversal of definition inheritors which might be a costly operation
   */
  def getTypeDefinitionIconWithKind(definition: ScTypeDefinition, baseFlags: Int = 0): Image = {
    val classKind = getScalaClassKind(definition)
    classKind match {
      case ScalaClassKind.Simple =>
        definition.getIconWithExtraLayerFlags(Iconable.ICON_FLAG_VISIBILITY, baseFlags)
      case ScalaClassKind.Runnable =>
        definition.getIconWithExtraLayerFlags(Iconable.ICON_FLAG_VISIBILITY, baseFlags | FLAGS_RUNNABLE)
      case ScalaClassKind.Test =>
        definition.getIconWithExtraLayerFlags(Iconable.ICON_FLAG_VISIBILITY, baseFlags | FLAGS_JUNIT_TEST)
      case ScalaClassKind.Exception if definition.hasAbstractModifier =>
        AbstractExceptionIcon
      case ScalaClassKind.Exception =>
        ExceptionIcon
    }
  }

  /**
   * Similar thing is done in Java version in [[com.intellij.psi.impl.ElementPresentationUtil.getFlags]]
   */
  private def getScalaClassKind(element: PsiModifierListOwner): ScalaClassKind = element match {
    case obj: ScTypeDefinition =>
      cachedInUserData("ScalaElementPresentationUtil.getRunnableFlags", obj, BlockModificationTracker(obj)) {
        getScalaClassKindImpl(obj)
      }
    case _ =>
      ScalaClassKind.Simple
  }

  /**
   * Similar logic for Java is located in [[com.intellij.psi.impl.ElementPresentationUtil.getClassKind]]
   * however we can't use the method as is because it can not handle the case when trait/enum have companion object with main method
   */
  private def getScalaClassKindImpl(clazz: ScTypeDefinition): ScalaClassKind = {
    if (DumbService.getInstance(clazz.getProject).isDumb)
      return ScalaClassKind.Simple

    if (ScalaMainMethodUtil.hasMainMethodFromProvidersOnly(clazz))
      return ScalaClassKind.Runnable

    val javaLangThrowable = JavaPsiFacade.getInstance(clazz.getProject).findClass(CommonClassNames.JAVA_LANG_THROWABLE, clazz.getResolveScope)
    val isException = javaLangThrowable != null && InheritanceUtil.isInheritorOrSelf(clazz, javaLangThrowable, true)
    if (isException)
      return ScalaClassKind.Exception

    if (TestFrameworks.getInstance.isTestClass(clazz))
      return ScalaClassKind.Test

    ScalaClassKind.Simple
  }

  private sealed trait ScalaClassKind
  private object ScalaClassKind {
    object Simple extends ScalaClassKind
    object Runnable extends ScalaClassKind
    object Exception extends ScalaClassKind
    object Test extends ScalaClassKind
  }
}
