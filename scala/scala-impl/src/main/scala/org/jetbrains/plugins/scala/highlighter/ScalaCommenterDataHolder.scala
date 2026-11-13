package org.jetbrains.plugins.scala.highlighter

import consulo.language.CommenterDataHolder
import consulo.util.dataholder.Key
import consulo.language.psi.PsiFile

private[highlighter]
class ScalaCommenterDataHolder(file: PsiFile) extends CommenterDataHolder {
  putUserData(ScalaCommenterDataHolder.PSI_FILE_KEY, file)

  def getPsiFile: PsiFile = getUserData(ScalaCommenterDataHolder.PSI_FILE_KEY)
}

private[highlighter]
object ScalaCommenterDataHolder {
  private val PSI_FILE_KEY = Key.create[PsiFile]("SCALA_COMMENTER_PSI_FILE")
}