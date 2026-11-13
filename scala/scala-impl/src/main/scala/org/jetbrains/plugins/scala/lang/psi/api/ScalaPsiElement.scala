package org.jetbrains.plugins.scala.lang.psi.api

import consulo.language.impl.psi.PsiFileBase
import consulo.util.dataholder.Key
import consulo.language.ast.{IElementType, TokenSet}
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import consulo.language.psi.util.PsiTreeUtil.isContextAncestor
import org.jetbrains.plugins.scala.extensions.Valid
import org.jetbrains.plugins.scala.lang.psi.ElementScope
import org.jetbrains.plugins.scala.lang.psi.types.Context
import org.jetbrains.plugins.scala.project.{ProjectContext, ProjectContextOwner}

import scala.collection.immutable.ArraySeq
import scala.reflect.{ClassTag, classTag}

trait ScalaPsiElement extends PsiElement
  with ProjectContextOwner {

  protected implicit def thisContext: Context = Context(this)

  import ScalaPsiElement._

  implicit def elementScope: ElementScope = ElementScope(this)

  override implicit def projectContext: ProjectContext = this.getProject

  final def isInCompiledFile: Boolean = getContainingFile match {
    case sf: ScalaFile => sf.isCompiled
    case _ => false
  }

  abstract override def getContext: PsiElement = this.context match {
    case null => super.getContext
    case element => element
  }

  def getSameElementInContext: PsiElement = this.child match {
    case null => this
    case element => element
  }

  def getDeepSameElementInContext: PsiElement = this.child match {
    case null                                            => this
    case child if child == this || child == this.context => this
    case child: ScalaPsiElement                          => child.getDeepSameElementInContext
    case child                                           => child
  }

  def startOffsetInParent: Int = this.child match {
    case element: ScalaPsiElement => element.startOffsetInParent
    case _ => getStartOffsetInParent
  }

  protected def findChildByClassScala[T >: Null <: ScalaPsiElement](clazz: Class[T]): T

  protected def findChildrenByClassScala[T >: Null <: ScalaPsiElement](clazz: Class[T]): Array[T]

  protected final def findChild[T >: Null <: ScalaPsiElement: ClassTag]: Option[T] =
    Option(findChildByClassScala(classTag[T].runtimeClass.asInstanceOf[Class[T]]))

  protected final def findChildren[T >: Null <: ScalaPsiElement: ClassTag]: Seq[T] =
    ArraySeq.unsafeWrapArray(findChildrenByClassScala(classTag[T].runtimeClass.asInstanceOf[Class[T]]))

  def findLastChildByTypeScala[T <: PsiElement](t: IElementType): Option[T] = {
    var node = getNode.getLastChildNode
    while (node != null && node.getElementType != t) {
      node = node.getTreePrev
    }
    Option(node).map(_.getPsi.asInstanceOf[T])
  }

  def findFirstChildByType(t: IElementType): Option[PsiElement] = {
    var node = getNode.getFirstChildNode
    while (node != null && node.getElementType != t) {
      node = node.getTreeNext
    }
    Option(node).map(_.getPsi)
  }

  def findFirstChildByType(set: TokenSet): Option[PsiElement] = {
    var node = getNode.getFirstChildNode
    while (node != null && !set.contains(node.getElementType)) {
      node = node.getTreeNext
    }
    Option(node).map(_.getPsi)
  }

  def findFirstChildByTypeScala[T <: PsiElement](t: IElementType): Option[T] =
    findFirstChildByType(t).map(_.asInstanceOf[T])

  def findChildrenByType(t: IElementType): Seq[PsiElement] = {
    val builder = Seq.newBuilder[PsiElement]
    var node = getNode.getFirstChildNode
    while (node != null) {
      if (node.getElementType == t) builder += node.getPsi
      node = node.getTreeNext
    }
    builder.result()
  }

  def findLastChildByType(set: TokenSet): Option[PsiElement] = {
    var node = getNode.getLastChildNode
    while (node != null && !set.contains(node.getElementType)) {
      node = node.getTreePrev
    }
    Option(node).map(_.getPsi)
  }

  protected def findLastChild[T >: Null <: ScalaPsiElement: ClassTag]: Option[T] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    var child = getLastChild
    while (child != null && !clazz.isInstance(child)) {
      child = child.getPrevSibling
    }
    Option(child.asInstanceOf[T])
  }

  abstract override def accept(visitor: PsiElementVisitor): Unit = visitor match {
    case visitor: ScalaElementVisitor => acceptScala(visitor)
    case _ => super.accept(visitor)
  }

  protected def acceptScala(visitor: ScalaElementVisitor): Unit = {
    visitor.visitScalaElement(this)
  }

  def acceptChildren(visitor: ScalaElementVisitor): Unit = getChildren.foreach {
    case element: ScalaPsiElement => element.accept(visitor)
    case _ =>
  }
}

object ScalaPsiElement {

  private val ContextKey = Key.create[PsiElement]("context.key")
  private val ChildKey = Key.create[PsiElement]("child.key")

  implicit class ScalaPsiElementExt(private val element: ScalaPsiElement) extends AnyVal {

    def context: PsiElement = getIfValid(ContextKey)

    def context_=(context: PsiElement): Unit = {
      assert(context == null || !isContextAncestor(element, context, /*strict*/ false))

      update(ContextKey, context)
    }

    def child: PsiElement = getIfValid(ChildKey)

    def child_=(child: PsiElement): Unit = {
      assert(child != element)

      update(ChildKey, child)
    }

    private def getIfValid(key: Key[PsiElement]): PsiElement = {
      val fromUserData = element match {
        case file: PsiFileBase => file.getCopyableUserData(key)
        case _                 => element.getUserData(key)
      }
      fromUserData match {
        case Valid(e) => e
        case _ =>
          update(key, null) //free reference to invalid element
          null
      }
    }

    private def update(key: Key[PsiElement], value: PsiElement): Unit = {
      element match {
        case file: PsiFileBase => file.putCopyableUserData(key, value)
        case _                 => element.putUserData(key, value)
      }
    }
  }

}
