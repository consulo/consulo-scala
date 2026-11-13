package org.jetbrains.plugins.scala.projectView

import consulo.project.ui.view.tree.{TreeStructureProvider, ViewSettings}
import consulo.application.dumb.DumbAware
import consulo.project.Project
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

import java.util
import scala.jdk.CollectionConverters._


/**
 * upstream: also shortened sbt module names and regrouped sbt modules in the project view
 * (ProjectViewModuleGroupNode, ProjectViewModuleNode, ProjectViewProjectNode, ModuleGrouper - impl classes in Consulo,
 * sbt module data from org.jetbrains.sbt.project)
 */
final class ScalaTreeStructureProvider extends TreeStructureProvider with DumbAware {

  import ScalaTreeStructureProvider._

  override def modify(parent: Node, children: util.Collection[Node], settings: ViewSettings): util.Collection[Node] = {
    val project = parent.getProject
    if (project == null) return children

    val childrenSeq = children.asScala.toSeq
    val modifiedChildren = childrenSeq.map { it => transform(it)(using it.getProject, settings) }
    modifiedChildren.asJavaCollection
  }
}

private object ScalaTreeStructureProvider {

  private def transform(node: Node)
                       (implicit project: Project, settings: ViewSettings): Node = {
    val nodeValue = node.getValue
    nodeValue match {
      case file: ScalaFile =>
        Node(file)
      case definition: ScTypeDefinition  =>
        node match {
          case _: TypeDefinitionNode =>
            node
          case _ =>
            //Scala type definition can be wrapped into non-TypeDefinitionNode in some other places in the platform
            //For example in com.intellij.ide.projectView.impl.ClassesTreeStructureProvider.doModify
            //ClassTreeNode is returned if file contains single class/trait/object definition
            //This is done in case file name equals to the type name
            //And this is even if the file contains other top level definitions in Scala 3 (def, val, etc...)
            //In this workaround we recalculate node for any definition which happens to be in Scala File
            if (definition.isTopLevel)
              definition.getContainingFile match {
                case file: ScalaFile =>
                  Node(file)
                case _ =>
                  new TypeDefinitionNode(definition)
              }
            else
              new TypeDefinitionNode(definition)
        }
      case _ =>
        node
    }
  }
}
