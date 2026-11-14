package org.jetbrains.plugins.scala.testingSupport.test.utest

import consulo.content.base.BinariesOrderRootType
import consulo.content.library.Library
import consulo.module.Module
import consulo.util.io.JarUtil
import consulo.virtualFileSystem.archive.ArchiveVfsUtil
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.caches.cachedInUserData
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration
import org.jetbrains.plugins.scala.project.{LibraryExt, ModuleExt, ProjectPsiElementExt}
import org.jetbrains.plugins.scala.rt.project.Version

import java.io.File
import java.util.jar.Attributes

private[testingSupport]
object UTestVersionUtils {

  private val UTestMavenCoordinates = "com.lihaoyi:utest"

  val Version090 = Version("0.9.0")

  /**
   * @param originalPsiElement psi element in which the corresponding action was invoked (e.g. "Create Test" action)
   */
  def getUTestLibraryVersionForTestModuleOf(originalPsiElement: PsiElement): Option[Version] = {
    val originalActionModule = originalPsiElement.module
    // upstream: CreateTestAction.suggestModuleForTests (separate production and test modules), a Consulo module has both
    val correspondingModuleForTests = originalActionModule
    correspondingModuleForTests.flatMap(UTestVersionUtils.getUTestLibraryVersion)
  }

  /**
   * ATTENTION: Library version detection currently works only for SBT, JPS, Maven, Gradle projects.
   * It does NOT work for BSP
   *  - For an SBT/Gradle/Maven project it depends on the registered maven coordinates during the project import
   *  - For JPS the logic is based on the library name (com.intellij.java.library.JavaLibraryUtil#getJpsLibraryVersion
   *
   * @note If you have an instance of production module, you can extract the corresponding test module using
   *       [[com.intellij.testIntegration.createTest.CreateTestAction.suggestModuleForTests]]
   */
  def getUTestLibraryVersion(module: Module): Option[Version] = cachedInUserData("UTestUtils.getUTestLibraryVersion", module, ScalaCompilerConfiguration.modTracker(module.getProject)) {
    if (module.isDisposed || module.getProject.isDefault) None
    else module.scalaMinorVersion.flatMap { scalaVersion =>
      val coordinatesWithSuffix = UTestMavenCoordinates + libraryCrossSuffix(scalaVersion)
      // upstream: JavaLibraryUtil.getLibraryVersion (maven coordinates of the library), here guessed from the library name
      val libraries = module.libraries
      val versionFromName = libraries.iterator
        .filter(_.getName != null)
        .filter(_.getName.contains(coordinatesWithSuffix + ":"))
        .flatMap(_.libraryVersion)
        .nextOption()
      val version = versionFromName.orElse(jarManifestVersion(libraries, coordinatesWithSuffix))
      version.map(Version(_))
    }
  }

  // upstream: JavaLibraryUtil.getJpsLibraryVersion
  private def jarManifestVersion(libraries: Iterable[Library], mavenCoordinates: String): Option[String] = {
    val artifactName = mavenCoordinates.substring(mavenCoordinates.indexOf(':') + 1)
    libraries.iterator
      .flatMap(_.getFiles(BinariesOrderRootType.ID))
      .filter(file => matchLibraryName(file.getNameWithoutExtension, artifactName))
      .flatMap(file => Option(ArchiveVfsUtil.getVirtualFileForArchive(file)))
      .flatMap(jar => Option(JarUtil.getJarAttribute(new File(jar.getPath), Attributes.Name.IMPLEMENTATION_VERSION)))
      .nextOption()
  }

  private def matchLibraryName(fileName: String, libraryName: String): Boolean =
    fileName == libraryName || {
      val prefix = libraryName + "-"
      fileName.length > prefix.length && fileName.startsWith(prefix) && fileName.charAt(prefix.length).isDigit
    }

  private def libraryCrossSuffix(scalaVersion: ScalaVersion): String =
    if (scalaVersion.isScala3)
      "_3"
    else
      "_" + scalaVersion.major
}
