package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import consulo.language.Language
import consulo.virtualFileSystem.{StandardFileSystems, VirtualFile}
import consulo.language.psi.stub.{DefaultStubBuilder, IStubFileElementType, IndexSink, PsiFileStub, PsiFileStubImpl, StubElement, StubInputStream, StubOutputStream}
import com.intellij.java.language.psi.PsiClass
import consulo.language.ast.ASTNode
import consulo.language.parser.{ParserDefinition, PsiBuilderFactory}
import consulo.language.psi.{PsiElement, PsiFile}
import consulo.language.version.LanguageVersion
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilderImpl
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.lang.TokenSets
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.compiled.ScClassFileDecompiler
import org.jetbrains.plugins.scala.lang.psi.stubs.ScFileStub

// upstream: a plain IFileElementType with stub support via LanguageStubDefinition and a file stub serializer
//noinspection TypeAnnotation
class ScStubFileElementType(debugName: String, language: Language)
  extends IStubFileElementType[ScFileStub](debugName, language) {

  override final def getExternalId: String = super.toString

  /**
   * Stub serialization format version for this language
   */
  override final def getStubVersion: Int =
    super.getStubVersion + ScStubFileElementType.BaseStubVersion + ScClassFileDecompiler.ScClsStubBuilder.getStubVersion

  override def shouldBuildStubFor(file: VirtualFile): Boolean =
    file.getFileSystem.getProtocol != StandardFileSystems.JAR_PROTOCOL

  override def getBuilder = new ScFileStubBuilderImpl

  override final def serialize(stub: ScFileStub, dataStream: StubOutputStream): Unit = {}

  override final def deserialize(dataStream: StubInputStream, parentStub: StubElement[?]): ScFileStub =
    new ScFileStubImpl(null)

  override final def indexStub(stub: PsiFileStub[?], sink: IndexSink): Unit = {}

  protected class ScFileStubBuilderImpl extends DefaultStubBuilder {

    override def buildStubTree(file: PsiFile): PsiFileStubImpl[? <: PsiFile] =
      super.buildStubTree(file).asInstanceOf[PsiFileStubImpl[? <: PsiFile]]

    protected override final def createStubForFile(file: PsiFile): PsiFileStubImpl[? <: PsiFile] =
      file.getViewProvider.getPsi(getLanguage) match {
        case scalaFile: ScalaFile => new ScFileStubImpl(scalaFile)
        case _ => new PsiFileStubImpl(file)
      }
  }

  protected final class ScFileStubImpl(@Nullable file: ScalaFile)
    extends PsiFileStubImpl(file) with ScFileStub {

    override def getType: IStubFileElementType[?] = ScStubFileElementType.this

    override def getClasses: Array[PsiClass] = getChildrenByType(
      TokenSets.TYPE_DEFINITIONS,
      PsiClass.ARRAY_FACTORY
    )
  }

}

object ScStubFileElementType {

  def apply(language: Language) = new ScStubFileElementType(
    s"${language.getID.toLowerCase} FILE".replace(' ', '.'),
    language
  )

  /**
   * Should be incremented each time when stub tree changes (e.g. elements added/removed,
   * element serialization/deserialization changes)
   */
  private val BaseStubVersion = 2
}
