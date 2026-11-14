package org.jetbrains.jps.incremental.scala.local.worksheet.util

import java.io.{OutputStream, PrintStream}

private[worksheet] object IOUtils {

  /**
   * upstream: System.out of the compile server is a Nailgun ThreadLocalPrintStream, here System.out of the IDE process
   * is replaced once by a stream which prints to the original System.out unless a worksheet runs in the current thread
   * (or in a thread started by it)
   */
  def patchSystemOut(out: OutputStream): Unit =
    ThreadLocalOutputStream.install().streams.set(out)

  def resetSystemOut(): Unit =
    ThreadLocalOutputStream.installed.foreach(_.streams.remove())

  private final class ThreadLocalOutputStream(default: PrintStream) extends OutputStream {
    val streams = new InheritableThreadLocal[OutputStream]

    private def current: OutputStream = {
      val stream = streams.get
      if (stream == null) default else stream
    }

    override def write(b: Int): Unit = current.write(b)
    override def write(b: Array[Byte], off: Int, len: Int): Unit = current.write(b, off, len)
    override def flush(): Unit = current.flush()
  }

  private object ThreadLocalOutputStream {
    private var stream: Option[ThreadLocalOutputStream] = None

    def installed: Option[ThreadLocalOutputStream] = synchronized(stream)

    def install(): ThreadLocalOutputStream = synchronized {
      stream.getOrElse {
        val threadLocal = new ThreadLocalOutputStream(System.out)
        System.setOut(new PrintStream(threadLocal, true))
        stream = Some(threadLocal)
        threadLocal
      }
    }
  }
}
