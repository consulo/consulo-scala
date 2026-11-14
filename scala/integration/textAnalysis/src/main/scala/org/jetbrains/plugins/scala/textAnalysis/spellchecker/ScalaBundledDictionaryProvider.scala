package org.jetbrains.plugins.scala.textAnalysis.spellchecker

import consulo.annotation.component.ExtensionImpl
import consulo.language.spellchecker.editor.dictionary.BundledDictionaryProvider

import java.io.InputStream
import java.util.function.BiConsumer

// upstream: getBundledDictionaries returns the dictionary names, Consulo providers load the dictionaries themselves
@ExtensionImpl
class ScalaBundledDictionaryProvider extends BundledDictionaryProvider {
  override def loadDictionaries(consumer: BiConsumer[String, InputStream]): Unit =
    ScalaBundledDictionaryProvider.Dictionaries.foreach { dictionary =>
      val stream = getClass.getResourceAsStream(dictionary)
      if (stream != null)
        consumer.accept(dictionary, stream)
    }
}

private object ScalaBundledDictionaryProvider {
  private val Dictionaries = Seq(
    "scala.dic",
    "awesome-scala-lib-name-parts.dic",
    "awesome-scala-org-name-parts.dic",
  )
}
