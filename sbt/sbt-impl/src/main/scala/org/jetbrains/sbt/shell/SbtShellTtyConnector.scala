package org.jetbrains.sbt.shell

import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector
import com.pty4j.{PtyProcess, WinSize}
import consulo.logging.Logger
import consulo.process.event.{ProcessEvent, ProcessListener}
import consulo.process.NativeProcessHandler
import consulo.util.dataholder.Key

import java.io.IOException
import java.nio.charset.Charset

/**
 * Connects the terminal of the terminal-based sbt shell to the sbt process.
 *
 * The output is not read from the process directly: the process handler keeps reading it and notifying its listeners
 * (the sbt shell communication listens to the same output), the connector receives it as a process listener and hands it
 * to the terminal. The input typed in the terminal is written to the process.
 *
 * The connector enforces a minimum number of rows: if the terminal rows fall at or below the minimum value used by
 * `org.jline.reader.impl.LineReaderImpl` (JLine 3), JLine switches to a mode where output is displayed on a single line:
 *   - commands are not fully rendered, e.g. during project sync, parts of the command are replaced with "..."
 *   - if minimized to 0 rows, the prompt becomes "...>", which breaks the logic for detecting when the sbt shell is ready for input
 *
 * The process lifecycle belongs to [[SbtProcessManager]], closing the connector (when the terminal is disposed) only
 * disconnects the terminal.
 */
// upstream: TerminalExecutionConsole.attachToProcess with a ProcessHandlerTtyConnector overriding `resize`
final class SbtShellTtyConnector(processHandler: NativeProcessHandler, charset: Charset) extends TtyConnector with ProcessListener {

  import SbtShellTtyConnector.*

  private val buffer = new java.lang.StringBuilder
  private val lock = new Object
  @volatile private var closed = false
  @volatile private var terminated = false
  private var lastCharIsCR = false

  processHandler.addProcessListener(this)

  // upstream: the process handler reads the PTY output with BaseOutputReader.Options.forTerminalPtyProcess (no line
  // splitting) - the reader of a Consulo process handler always splits the output to lines and drops the CR of CRLF,
  // so line feeds are turned back into CRLF here (a lone CR, used by JLine to redraw the line, is kept by the reader)
  override def onTextAvailable(event: ProcessEvent, outputType: Key[?]): Unit =
    lock.synchronized {
      event.getText.foreach { char =>
        if (char == '\n' && !lastCharIsCR) buffer.append('\r')
        buffer.append(char)
        lastCharIsCR = char == '\r'
      }
      lock.notifyAll()
    }

  override def processTerminated(event: ProcessEvent): Unit =
    lock.synchronized {
      terminated = true
      lock.notifyAll()
    }

  override def read(buf: Array[Char], offset: Int, length: Int): Int =
    lock.synchronized {
      while (buffer.isEmpty && !closed && !terminated)
        lock.wait()
      if (buffer.isEmpty) -1
      else {
        val count = math.min(length, buffer.length)
        buffer.getChars(0, count, buf, offset)
        buffer.delete(0, count)
        count
      }
    }

  override def write(bytes: Array[Byte]): Unit = {
    val input = processHandler.getProcessInput
    if (input == null || processHandler.isProcessTerminated)
      throw new IOException("The sbt shell process is not running")
    input.write(bytes)
    input.flush()
  }

  override def write(string: String): Unit =
    write(string.getBytes(charset))

  override def isConnected: Boolean =
    !closed && !processHandler.isProcessTerminated

  override def resize(termSize: TermSize): Unit =
    processHandler.getProcess match {
      case ptyProcess: PtyProcess if ptyProcess.isAlive =>
        val rows = math.max(termSize.getRows, JLineMinRows + 1)
        try ptyProcess.setWinSize(new WinSize(termSize.getColumns, rows))
        catch {
          case e: Exception => Log.debug("Can't resize the sbt shell PTY", e)
        }
      case _ =>
    }

  override def waitFor(): Int = {
    processHandler.waitFor()
    Option(processHandler.getExitCode).map(_.intValue).getOrElse(0)
  }

  override def ready(): Boolean =
    lock.synchronized(!buffer.isEmpty)

  override def getName: String = "sbt shell"

  override def close(): Unit = {
    processHandler.removeProcessListener(this)
    lock.synchronized {
      closed = true
      lock.notifyAll()
    }
  }
}

object SbtShellTtyConnector {

  private val Log = Logger.getInstance(classOf[SbtShellTtyConnector])

  // from org.jline.reader.impl.LineReaderImpl.MIN_ROWS
  private val JLineMinRows = 3
}
