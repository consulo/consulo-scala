package org.jetbrains.plugins.scala.gotoclass

import consulo.annotation.component.ExtensionImpl

import consulo.project.DumbService
import consulo.language.psi.PsiElement
import consulo.language.statistician.StatisticsInfo
import consulo.language.util.proximity.ProximityLocation
import consulo.language.util.proximity.ProximityStatistician
import org.jetbrains.plugins.scala.lang.completion.statistician.ScalaStatisticManager

@ExtensionImpl(order = "first")
class ScalaProximityStatistician extends ProximityStatistician {
  override def serialize(element: PsiElement, location: ProximityLocation): StatisticsInfo = {
    if (DumbService.isDumb(element.getProject)) return null

    ScalaStatisticManager.memberKey(element)
      .map(el => new StatisticsInfo("scalaMember#", el))
      .orNull
  }
}
