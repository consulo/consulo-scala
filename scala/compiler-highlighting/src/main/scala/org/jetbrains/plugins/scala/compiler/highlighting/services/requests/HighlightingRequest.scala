package org.jetbrains.plugins.scala.compiler.highlighting.services.requests

import consulo.virtualFileSystem.VirtualFile

import java.util.concurrent.atomic.AtomicLong

/**
 * upstream: also carries the request id of the tracing (compiler highlighting tracing is not ported)
 */
trait HighlightingRequest:

  val sequence: Long

  /** Human-readable reason the request was triggered, used for logging. */
  def debugReason: String

  /** The file on which the highlighting has been requested */
  val file: VirtualFile

object HighlightingRequest:

  given Ordering[HighlightingRequest] = Ordering.by[HighlightingRequest, Long](_.sequence).reverse

  private object RequestClock:
    private val sequence = new AtomicLong(0L)
    def nextTick(): Long = sequence.incrementAndGet()

  private case class HighlightingRequestImpl(override val file: VirtualFile,
                                             override val debugReason: String,
                                             override val sequence: Long) extends HighlightingRequest

  def apply(file: VirtualFile, debugReason: String): HighlightingRequest =
    HighlightingRequestImpl(file, debugReason, RequestClock.nextTick())
