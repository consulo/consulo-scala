package org.jetbrains.plugins.scala.lang.completion.weighter

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.{CompletionLocation, CompletionWeigher}
import consulo.language.editor.completion.lookup.LookupElement
import consulo.util.dataholder.Key
import consulo.language.psi.util.PsiTreeUtil.getContextOfType
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.{PsiElement, PsiNamedElement}
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.extensions.{&, PsiNamedElementExt, ToNullSafe}
import org.jetbrains.plugins.scala.lang.completion.{insideTypePattern, positionFromParameters}
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScAssignment, ScNewTemplateDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScPatternDefinition, ScTypeAlias, ScTypedDeclaration}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

/**
 * Suggest a type by name where a type may appear. Check on full equality of names, includence one to another,
 * or partial alignment
 * on 3/2/16
 */
@ExtensionImpl(id = "scalaByNameWeigher", order = "before stats")
final class ScalaByNameWeigher extends CompletionWeigher {

  import ScalaByNameWeigher._

  override def weigh(element: LookupElement, location: CompletionLocation): Comparable[?] = {
    val parameters = location.getCompletionParameters
    val position = positionFromParameters(using parameters)

    def handleByText(@NotNull name: String): Option[Integer] = {
      val maybeNameAtPosition = parameters.getOriginalPosition match {
        case null => None
        case originalPosition => Option(originalPosition.getUserData(TextForPositionKey))
      }

      val maybeName = maybeNameAtPosition match {
        case None =>
          val result = asBindingPattern(position)
            .orElse(afterColonType(position))
            .orElse(afterNew(position))

          result.foreach {
            position.putCopyableUserData(TextForPositionKey, _)
          }

          result
        case result => result
      }

      maybeName.filterNot(_.isEmpty).map {
        case text if text.length == 1 && text(0).toUpper == name(0) => 0
        case text if text.toUpperCase == name.toUpperCase => 0
        case text =>
          computeDistance(name, text) match {
            case -1 => null
            case distance => -distance
          }
      }
    }

    element.getPsiElement match {
      case (namedElement: PsiNamedElement) & (_: ScTypeAlias |
                         _: ScTypeDefinition |
                         _: PsiClass |
                         _: ScParameter)
        if insideTypePattern.accepts(position, location.getProcessingContext) =>
        Option(namedElement.name)
          .flatMap(handleByText)
          .orNull
      case _ => null
    }
  }
}

private object ScalaByNameWeigher {

  private val MaxDistance = 4
  private val TextForPositionKey = Key.create[String]("text.for.position")

  private def computeDistance(name: String, text: String): Int = {
    // prevent MAX_DISTANCE be more or equals on of comparing strings
    val maxDist = Math.min(MaxDistance, Math.ceil(Math.max(text.length, name.length) / 2))

    // prevent computing distance on long non-including strings
    if (Math.abs(text.length - name.length) > maxDist)
      -1
    else {
      val distance = optimalAlignment(name, text, false)
      if (distance > maxDist) -1 else distance
    }
  }

  private def afterNew(place: PsiElement): Option[String] =
    getContextOfType(place, classOf[ScNewTemplateDefinition]) match {
      case null => None
      case newTemplateDefinition =>
        newTemplateDefinition.getContext match {
          case patterDef: ScPatternDefinition =>
            patterDef.bindings.headOption.map(_.name)
          case assignment: ScAssignment =>
            assignment.referenceName
          case _ => None
        }
    }

  private def afterColonType(place: PsiElement): Option[String] =
    place.getContext.nullSafe.map(_.getContext).map(_.getContext).orNull match {
      case typedDeclaration: ScTypedDeclaration =>
        typedDeclaration.declaredElements.headOption.map(_.name)
      case _ => None
    }

  //case label name
  private def asBindingPattern(place: PsiElement): Option[String] =
    place.getContext.nullSafe.map(_.getContext).map(_.getContext).map(_.getContext).orNull match {
      case bp: ScBindingPattern => Some(bp.name)
      case _ => None
    }

  // upstream: com.intellij.util.text.EditDistance.optimalAlignment (optimal string alignment distance)
  private def optimalAlignment(str1: String, str2: String, caseSensitive: Boolean): Int = {
    def equal(c1: Char, c2: Char) =
      if (caseSensitive) c1 == c2 else Character.toLowerCase(c1) == Character.toLowerCase(c2)

    val (s1, s2) = if (str1.length > str2.length) (str2, str1) else (str1, str2)
    val length1 = s1.length
    if (length1 == 0) return s2.length

    var v0 = new Array[Int](length1 + 1)
    var v1 = Array.tabulate(length1 + 1)(identity)
    var v2 = new Array[Int](length1 + 1)
    for (j <- 0 until s2.length) {
      v2(0) = j + 1
      for (i <- 0 until length1) {
        val cost = if (equal(s1.charAt(i), s2.charAt(j))) 0 else 1
        v2(i + 1) = math.min(math.min(v2(i) + 1, v1(i + 1) + 1), v1(i) + cost)
        if (i > 0 && j > 0 && equal(s2.charAt(j), s1.charAt(i - 1)) && equal(s1.charAt(i), s2.charAt(j - 1)))
          v2(i + 1) = math.min(v2(i + 1), v0(i - 1) + cost)
      }
      val temp = v0
      v0 = v1
      v1 = v2
      v2 = temp
    }
    v1(length1)
  }
}
