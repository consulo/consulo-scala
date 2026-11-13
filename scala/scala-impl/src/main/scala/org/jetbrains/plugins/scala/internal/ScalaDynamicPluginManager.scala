package org.jetbrains.plugins.scala.internal

// upstream: class ScalaDynamicPluginManager extends DynamicPluginListener (tracks unloading of the Scala plugin),
// Consulo doesn't unload plugins dynamically
object ScalaDynamicPluginManager {
  def isScalaPluginUnloading: Boolean = false
}
