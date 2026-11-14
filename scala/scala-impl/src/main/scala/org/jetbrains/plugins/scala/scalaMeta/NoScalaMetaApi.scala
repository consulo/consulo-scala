package org.jetbrains.plugins.scala.scalaMeta

import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScInterpolatedStringLiteral, ScReference}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.impl.base.patterns.ScInterpolationPatternImpl
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue.Parameter
import org.jetbrains.plugins.scala.lang.psi.types.result.{Failure, TypeResult}
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

/** Used while the scalameta integration (upstream scala-meta-impl module) is not ported: no meta quasiquotes. */
@Singleton
@ServiceImpl
final class NoScalaMetaApi extends ScalaMetaApi {
  override def isMetaQQ(ref: ScReference): Boolean = false
  override def isMetaQQ(fun: ScFunction): Boolean = false
  override def getMetaQQExpectedTypes(srr: ScalaResolveResult, stringContextApplicationRef: ScReferenceExpression): Seq[Parameter] = Seq.empty
  override def getMetaQQExprType(pat: ScInterpolatedStringLiteral): TypeResult =
    Failure("scalameta is not supported")(using pat.getProject)
  override def getMetaQQPatternTypes(pat: ScInterpolationPatternImpl): Seq[String] = Seq.empty
  override def extractScalaMetaParseException(throwable: Throwable): Option[(String, Int)] = None
}
