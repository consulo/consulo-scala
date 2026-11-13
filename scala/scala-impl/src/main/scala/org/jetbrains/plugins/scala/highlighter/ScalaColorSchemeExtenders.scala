package org.jetbrains.plugins.scala.highlighter

import consulo.annotation.component.ExtensionImpl
import consulo.colorScheme.{AttributesFlyweight, EditorColorSchemeExtender, EditorColorsScheme, TextAttributesKey}
import consulo.util.jdom.JDOMUtil

import scala.jdk.CollectionConverters._

/**
 * Scala additions to the bundled color schemes, read from the upstream scheme files in `colorSchemes/`
 * (upstream: `additionalTextAttributes` registrations).
 */
abstract class ScalaColorSchemeExtenderBase(resource: String) extends EditorColorSchemeExtender {
  override def extend(builder: EditorColorSchemeExtender.Builder): Unit = {
    val stream = getClass.getResourceAsStream(resource)
    if (stream == null) return
    val root = try JDOMUtil.load(stream) finally stream.close()
    if (root == null) return
    for {
      option <- root.getChildren("option").asScala
      value <- Option(option.getChild("value"))
    } builder.add(TextAttributesKey.of(option.getAttributeValue("name")), AttributesFlyweight.create(value))
  }
}

@ExtensionImpl
final class ScalaDefaultColorSchemeExtender extends ScalaColorSchemeExtenderBase("/colorSchemes/ScalaDefault.xml") {
  override def getColorSchemeId: String = EditorColorsScheme.DEFAULT_SCHEME_NAME
}

@ExtensionImpl
final class ScalaDarculaColorSchemeExtender extends ScalaColorSchemeExtenderBase("/colorSchemes/ScalaDarcula.xml") {
  override def getColorSchemeId: String = EditorColorsScheme.DARCULA_SCHEME_NAME
}
