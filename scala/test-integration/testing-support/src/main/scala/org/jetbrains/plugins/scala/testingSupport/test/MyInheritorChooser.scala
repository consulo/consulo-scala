package org.jetbrains.plugins.scala.testingSupport
package test

import consulo.execution.action.ConfigurationContext
import com.intellij.java.execution.impl.junit.InheritorChooser
import com.intellij.java.language.impl.codeInsight.PsiClassListCellRenderer
import consulo.fileEditor.{FileEditor, TextEditor}
import consulo.ui.ex.awt.popup.AWTPopupFactory
import consulo.ui.ex.popup.JBPopupFactory
import consulo.util.lang.function.Condition
import consulo.util.lang.StringUtil
import com.intellij.java.language.psi.{PsiClass, PsiClassOwner, PsiMethod}
import consulo.language.psi.PsiDocumentManager
import com.intellij.java.indexing.search.searches.ClassInheritorsSearch
import consulo.ui.ex.awt.JBList
import org.jetbrains.plugins.scala.extensions.PsiClassExt
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.testingSupport.test.testdata._

import javax.swing.ListCellRenderer
import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

private class MyInheritorChooser(
  config: AbstractTestRunConfiguration,
  testData: ClassTestData
) extends InheritorChooser() {

  override def runMethodInAbstractClass(
    context: ConfigurationContext,
    performRunnable: Runnable,
    psiMethod: PsiMethod,
    containingClass: PsiClass,
    acceptAbstractCondition: Condition[PsiClass]
  ): Boolean = {
    //TODO: SCL-10530 this is mostly copy-paste from InheritorChooser; get rid of this once we support pattern test runs
    if (containingClass == null) return false

    val classes = ClassInheritorsSearch.search(containingClass).asScala
      .filterNot(config.isInvalidSuite)
      .toList
    if (classes.isEmpty) return false

    if (classes.size == 1) {
      runForClass(classes.head, psiMethod, context, performRunnable)
      return true
    }

    val fileEditor = context.getDataContext.getData(FileEditor.KEY)
    fileEditor match {
      case editor: TextEditor =>
        val document = editor.getEditor.getDocument
        val containingFile = PsiDocumentManager.getInstance(context.getProject).getPsiFile(document)
        containingFile match {
          case owner: PsiClassOwner =>
            val psiClasses = owner.getClasses
            psiClasses.filter(classes.contains(_))
            if (psiClasses.size == 1) {
              runForClass(psiClasses.head, psiMethod, context, performRunnable)
              return true
            }
          case _ =>
        }
      case _ =>
    }

    val renderer  = new PsiClassListCellRenderer()
    val classesSorted = classes.sorted(using Ordering.comparatorToOrdering(using renderer.getComparator))
    val jbList = new JBList(classesSorted*)
    //scala type system gets confused because someone forgot generics in PsiElementListCellRenderer definition
    jbList.setCellRenderer(renderer.asInstanceOf[ListCellRenderer[PsiClass]])
    val testName = if (psiMethod != null) psiMethod.getName else containingClass.getName
    val pupupFactory = JBPopupFactory.getInstance().asInstanceOf[AWTPopupFactory].createListPopupBuilder(jbList)
    pupupFactory
      .setTitle(TestingSupportBundle.message("test.config.choose.executable.classes.to.run.test", testName))
      .setMovable(false)
      .setResizable(false)
      .setRequestFocus(true)
      .setItemChosenCallback((aClass: PsiClass) => {
        if (aClass != null)
          runForClass(aClass, psiMethod, context, performRunnable)
      })
    pupupFactory.createPopup().showInBestPositionFor(context.getDataContext)
    true
  }

  override protected def runForClass(
    aClass: PsiClass,
    psiMethod: PsiMethod,
    context: ConfigurationContext,
    performRunnable: Runnable
  ): Unit = {
    testData.testClassPath = aClass.qualifiedName
    config.setName(StringUtil.getShortName(aClass.qualifiedName) + (testData match {
      case single: SingleTestData => "." + single.testName
      case _ => ""
    }))
    Option(ScalaPsiUtil.getModule(aClass)).foreach(config.setModule)
    performRunnable.run()
  }
}
