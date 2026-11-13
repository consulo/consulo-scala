package org.jetbrains.plugins.scala.codeInsight.intention

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import com.intellij.java.impl.codeInsight.intention.impl.ImplementAbstractMethodAction

//TODO: create alternative intention actions for all kinds of members: val/var/type etc...
@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ImplementAbstractMethodIntention", fileExtensions = Array("scala"), categories = Array("Scala"))
class ImplementAbstractMethodIntention extends ImplementAbstractMethodAction
