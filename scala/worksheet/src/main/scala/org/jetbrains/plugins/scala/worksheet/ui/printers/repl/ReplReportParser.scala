package org.jetbrains.plugins.scala.worksheet.ui.printers.repl

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * upstream: org.jetbrains.jps.incremental.scala.local.worksheet.repl_interface.PrintWriterReporter.parse, the REPL
 * interface classes are loaded only by the compile engine (lib/jps/repl-interface.jar) - this is a copy of the
 * parsing of the messages printed by PrintWriterReporter.report
 */
private[printers] object ReplReportParser {

  final val IJReportPrefix = "###IJ_REPORT###"

  // severity, line, column, lineContentEncoded, messageEncoded
  private val IJReportRegexp = """(\w+):(\d+):(\d+):(.*?):(.*)""".r

  final case class MessageLineParsed(severity: String, line: Integer, column: Integer, lineContent: String, message: String)

  def parse(messageLine: String): MessageLineParsed = messageLine match {
    case IJReportRegexp(severity, line, column, lineContent, message) =>
      try MessageLineParsed(severity, line.toInt, column.toInt, decode(lineContent), decode(message))
      catch {
        case _: NumberFormatException => null
      }
    case _ => null
  }

  private def decode(text: String): String =
    new String(Base64.getDecoder.decode(text), StandardCharsets.UTF_8)
}
