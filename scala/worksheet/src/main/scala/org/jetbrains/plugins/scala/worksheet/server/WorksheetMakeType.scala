package org.jetbrains.plugins.scala.worksheet.server

// upstream: InProcessServer / OutOfProcessServer / NonServer - there is no compile server, the worksheet is always
// compiled in the IDE process by the compile engine
abstract sealed class WorksheetMakeType

case object InProcess extends WorksheetMakeType // the worksheet is compiled and run in the IDE process
case object OutOfProcess extends WorksheetMakeType // the worksheet is compiled in the IDE process and run in a separate JVM
