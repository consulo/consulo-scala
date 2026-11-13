package org.jetbrains.plugins.scala.lang.psi.compiled

import consulo.language.Language
import consulo.virtualFileSystem.fileType.FileType
import consulo.module.Module
import consulo.project.Project
import consulo.language.impl.psi.ResolveScopeManager
import scala.jdk.CollectionConverters._
import consulo.module.content.{ProjectFileIndex, ProjectRootManager}
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.search.FilenameIndex
import consulo.language.psi.scope.GlobalSearchScope
import com.intellij.java.language.psi.PsiClassOwner
import consulo.language.psi.{PsiElement, PsiFile, PsiManager}
import consulo.language.impl.file.SingleRootFileViewProvider
import org.jetbrains.plugins.scala.caches.cachedInUserData
import org.jetbrains.plugins.scala.extensions.{ClassQualifiedName, ObjectExt, PsiClassExt}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiImplementationHelper.findAssociatedEntities
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl
import org.jetbrains.plugins.scala.tasty.reader.CompilerOptions

import scala.annotation.tailrec

/**
 * For the non-decompiled scala files see [[org.jetbrains.plugins.scala.lang.psi.ScFileViewProvider]]
 */
final class ScClsFileViewProvider(
  decompilationResult: ScalaDecompilationResult
)(
  manager: PsiManager,
  file: VirtualFile,
  eventSystemEnabled: Boolean,
  language: Language
) extends SingleRootFileViewProvider(
  manager,
  file,
  eventSystemEnabled,
  language
) {

  private def sourceName: String = decompilationResult.sourceName

  override def getContents: String = decompilationResult.sourceText

  private def compilerOptions: CompilerOptions = decompilationResult.compilerOptions

  override def createFile(
    project: Project,
    file: VirtualFile,
    fileType: FileType
  ): PsiFile =
    new ScClsFileViewProvider.ScClsFileImpl(this)

  override def createCopy(file: VirtualFile) =
    new ScClsFileViewProvider(decompilationResult)(getManager, file, eventSystemEnabled = false, getBaseLanguage)
}

object ScClsFileViewProvider {

  /**
   * Java alternative is [[com.intellij.psi.impl.compiled.ClsFileImpl]],
   * though note that there are plenty of various differences in our implementations and approaches.
   */
  final class ScClsFileImpl(override val getViewProvider: ScClsFileViewProvider)
    extends ScalaFileImpl(getViewProvider) {

    private implicit def manager: PsiManager = getManager

    override def isCompiled: Boolean = true

    override def compilerOptions: Option[CompilerOptions] = Some(getViewProvider.compilerOptions)

    override def getVirtualFile: VirtualFile = getViewProvider.getVirtualFile

    override def getNavigationElement: PsiElement = {
      val sourceForCompiledFile: Option[VirtualFile] = cachedInUserData("getNavigationElement.sourceForCompiledFile", this, ProjectRootManager.getInstance(getProject)) {
        findSourceByRelativePath.orElse(findSourceByQualifiedName)
      }

      sourceForCompiledFile
        .flatMap(findPsiFile)
        .getOrElse(super.getNavigationElement)
    }

    override protected def defaultFileResolveScope(file: VirtualFile): GlobalSearchScope = cachedInUserData("defaultFileResolveScope",  this, ProjectRootManager.getInstance(getProject), Tuple1(file)) {
      // this cache is very inefficient when orderEntries.size is large
      ResolveScopeManager.getInstance(manager.getProject).getDefaultResolveScope(file)
    }

    def findSourceByRelativePath: Option[VirtualFile] = {
      val relPath = relativePath(typeDefinitions)
      val classEntities = findAssociatedEntities(getVirtualFile, projectFileIndex)

      findSourceFileWithName(getViewProvider.sourceName) {
        hasSameRelativePathInSources(_, relPath, classEntities)
      }
    }

    //Look in libraries sources if file not relative to path
    //this is more expensive because parsing of found source files is required
    private def findSourceByQualifiedName: Option[VirtualFile] = {
      val qualifiedName = typeDefinitions.headOption match {
        case Some(ClassQualifiedName(qualifiedName)) => qualifiedName
        case _                                       => return None
      }

      findSourceFileWithName(getViewProvider.sourceName) {
        hasClassWithQualifiedName(_, qualifiedName)
      }
    }

    private def relativePath(typeDefinitions: Seq[ScTypeDefinition]) = {
      val builder = new StringBuilder()

      buildPackagePath(firstPackaging, builder)

      for {
        packageObject <- typeDefinitions.find(_.isPackageObject)
        segment = packageObject.name
      } builder.appendSegment(segment)

      builder.append(getViewProvider.sourceName).toString
    }

    private def projectFileIndex: ProjectFileIndex = ProjectFileIndex.getInstance(manager.getProject)

    @tailrec
    private def buildPackagePath(maybePackaging: Option[ScPackaging], builder: StringBuilder): StringBuilder =
      maybePackaging match {
        case Some(packaging) if !packaging.isExplicit =>
          buildPackagePath(
            packaging.packagings.headOption,
            builder.appendSegments(packaging.packageName.split('.').toSeq)
          )
        case _ => builder
      }

    private def hasClassWithQualifiedName(sourceFile: VirtualFile, qualifiedName: String): Boolean = {
      val clazzIterator = findPsiFile(sourceFile) match {
        case Some(scalaFile: ScalaFile) => scalaFile.typeDefinitions.iterator
        case Some(classOwner: PsiClassOwner) => classOwner.getClasses.iterator
        case _ => Iterator.empty
      }

      clazzIterator.exists(_.qualifiedName == qualifiedName)
    }

    private def hasSameRelativePathInSources(sourceFile: VirtualFile,
                                             relPath: String,
                                             classFileEntries: Iterable[AnyRef]): Boolean =
      sourceFile.getPath.endsWith(relPath) && haveSameEntity(findAssociatedEntities(sourceFile, projectFileIndex), classFileEntries)

    //noinspection ExistsEquals
    private def haveSameEntity(firstEntities: Iterable[AnyRef], secondEntities: Iterable[AnyRef]): Boolean = {
      firstEntities.exists { e1 =>
        secondEntities.exists { e2 =>
          e1 == e2
        }
      }
    }

    private def findSourceFileWithName(name: String)
                                      (predicate: VirtualFile => Boolean): Option[VirtualFile] = {
      val project = getProject
      val fileIndex = projectFileIndex
      val sourcesScope = new GlobalSearchScope(project) {
        override def isSearchInModuleContent(aModule: Module): Boolean = false
        override def isSearchInLibraries: Boolean = true
        override def contains(file: VirtualFile): Boolean = fileIndex.isInSource(file)
      }
      val caseSensitively = true
      FilenameIndex.getVirtualFilesByName(
        project,
        name,
        caseSensitively,
        GlobalSearchScope.allScope(project).intersectWith(sourcesScope),
      ).asScala.find(item => !item.isDirectory && predicate(item))
    }

    private def findPsiFile(file: VirtualFile): Option[PsiClassOwner] =
      manager.findFile(file).asOptionOf[PsiClassOwner]
  }

  private implicit class StringBuilderExt(private val builder: StringBuilder) extends AnyVal {

    def appendSegment(segment: String): StringBuilder =
      builder.append(segment).append('/')

    def appendSegments(segments: Seq[String]): StringBuilder = {
      segments.foreach(appendSegment)
      builder
    }
  }

}
