package org.jetbrains.plugins.scala.structureView.element

import consulo.navigation.ItemPresentation
import consulo.ui.ex.awt.UIUtil

trait InheritedLocationStringItemPresentation extends ItemPresentation  { self: Element =>

  override final def getLocationString: String =
    if (inherited)
      location.map(UIUtil.rightArrow + _).orNull
    else
      null

  protected def location: Option[String] = None
}
