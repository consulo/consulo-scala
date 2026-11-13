package org.jetbrains.plugins.scala.gotoclass

import consulo.searchEverywhere.SEResultsEqualityProvider.SEEqualElementsActionType
import consulo.searchEverywhere.{SEResultsEqualityProvider, SearchEverywhereFoundElementInfo}
import consulo.language.psi.{PsiElement, PsiElementNavigationItem}
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.lang.psi.light.PsiClassWrapper

/**
 * @see Java implementation [[com.intellij.ide.JavaClassAndFileEqualityProvider]]
 * @see Kotlin implementation `org.jetbrains.kotlin.idea.searcheverywhere.KtSearchEverywhereEqualityProvider`
 */
// upstream: @ApiStatus.Internal
// The legacy Search Everywhere still invokes this extension; its replacement uses a different API.
@scala.annotation.nowarn("msg=(?s).*(SEResultsEqualityProvider|SearchEverywhereFoundElementInfo).*deprecated.*")
final class ScalaSearchEverywhereEqualityProvider extends SEResultsEqualityProvider {

  override def compareItems(
    newItem: SearchEverywhereFoundElementInfo,
    alreadyFoundItem: SearchEverywhereFoundElementInfo
  ): SEEqualElementsActionType =
    Option(getActionType(toPsi(newItem), alreadyFoundItem))
      .getOrElse(SEEqualElementsActionType.DO_NOTHING)

  /**
   * @return null if we don't want to choose any action for this pair
   */
  @Nullable
  private def getActionType(
    newPsiElement: PsiElement,
    alreadyFoundItem: SearchEverywhereFoundElementInfo
  ): SEEqualElementsActionType = {
    val alreadyFoundPsiElement = toPsi(alreadyFoundItem)
    alreadyFoundPsiElement match {
      case null =>
        null
      case wrapper: PsiClassWrapper if wrapper.definition eq newPsiElement =>
        SEEqualElementsActionType.REPLACE
      case _ =>
        newPsiElement match {
          case wrapper: PsiClassWrapper if wrapper.definition eq alreadyFoundPsiElement =>
            SEEqualElementsActionType.SKIP
          case _ =>
            null
        }
    }
  }

  private def toPsi(item: SearchEverywhereFoundElementInfo): PsiElement = item.getElement match {
    case psi: PsiElement => psi
    case navigationItem: PsiElementNavigationItem => navigationItem.getTargetElement
    case _ => null
  }
}
