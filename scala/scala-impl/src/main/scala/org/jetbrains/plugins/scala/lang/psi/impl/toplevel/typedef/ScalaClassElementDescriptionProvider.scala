package org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.{ElementDescriptionLocation, ElementDescriptionProvider, PsiElement}
import consulo.usage.UsageViewTypeLocation
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.psi.light.PsiClassWrapper

//ATTENTION!!!
//It's not clear what was the original primary purpose of this class
//This provider doesn't use "location" and returns name in too many possible locations
//Ideally it should be specific on which locations it should handle
@ExtensionImpl
class ScalaClassElementDescriptionProvider extends ElementDescriptionProvider {
  override def getElementDescription(element: PsiElement, location: ElementDescriptionLocation): LocalizeValue = {
    if (location == UsageViewTypeLocation.INSTANCE)
      return LocalizeValue.empty() //Handled by consulo.usage.UsageViewTypeLocation.DEFAULT_PROVIDER

    element match {
      case o: ScObject => LocalizeValue.ofNullable(o.name)
      case PsiClassWrapper(definition) => LocalizeValue.ofNullable(definition.name)
      case _ => LocalizeValue.empty()
    }
  }
}
