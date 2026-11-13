package org.jetbrains.plugins.scala.help

// upstream: class ScalaWebHelpProvider extends WebHelpProvider (maps help topics with HelpPrefix to
// version-specific https://www.jetbrains.com/help/idea/ pages), Consulo has no pluggable web help providers
object ScalaWebHelpProvider {
  val HelpPrefix = "org.jetbrains.plugins.scala."
}
