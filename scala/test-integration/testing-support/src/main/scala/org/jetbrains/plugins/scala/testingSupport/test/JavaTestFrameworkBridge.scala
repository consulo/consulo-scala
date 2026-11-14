package org.jetbrains.plugins.scala.testingSupport.test

import consulo.fileTemplate.FileTemplateDescriptor
import consulo.module.Module
import com.intellij.java.language.psi.{PsiClass, PsiMethod}
import com.intellij.java.language.testIntegration.JavaTestFramework
/**
 * This intermediate class is added to mute/hide some base methods from JavaTestFramework
 * TODO: investigate possibility to extend TestFramework instead of JavaTestFramework cause it has too many redundant methods
 */
abstract class JavaTestFrameworkBridge extends JavaTestFramework {

  /**
   * upstream: a dummy non-null value to make the "Fix" button in "Create test dialog" call `setupLibrary`.
   * Consulo's dialog adds the jar of `getLibraryPath` to the module roots instead, so no path is provided.
   */
  override final def getLibraryPath: String = null

  override final def setupLibrary(module: Module): Unit =
    this match {
      case framework: TestFrameworkSetupSupport => framework.setupFramework(module)
      case _                                    =>
    }

  override def getMnemonic: Char = 0

  //
  // Explicitly override with dummy values: Scala Plugin doesn't use these features,
  //

  // something from automatic generation of test methods, classes, setup, teardown...
  override final def getTestMethodFileTemplateDescriptor: FileTemplateDescriptor = null // TODO: @NotNull in platform
  override final def getTestClassFileTemplateDescriptor: FileTemplateDescriptor = null
  override final def getParametersMethodFileTemplateDescriptor: FileTemplateDescriptor = null
  override final def getSetUpMethodFileTemplateDescriptor: FileTemplateDescriptor = null
  override final def getTearDownMethodFileTemplateDescriptor: FileTemplateDescriptor = null

  override protected final def findSetUpMethod(clazz: PsiClass): PsiMethod = null
  override protected final def findTearDownMethod(clazz: PsiClass): PsiMethod = null
  override protected final def findOrCreateSetUpMethod(clazz: PsiClass): PsiMethod = null
}
