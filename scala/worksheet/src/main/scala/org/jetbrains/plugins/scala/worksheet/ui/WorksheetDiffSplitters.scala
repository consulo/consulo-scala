package org.jetbrains.plugins.scala.worksheet.ui

import consulo.application.ApplicationManager
import consulo.logging.Logger
import consulo.codeEditor.Editor
import consulo.codeEditor.event.{VisibleAreaEvent, VisibleAreaListener}
import consulo.ui.ex.awt.{Divider, Splitter}
import consulo.util.dataholder.Key
import consulo.language.psi.PsiDocumentManager
import consulo.ui.ex.JBColor
import consulo.ui.annotation.{RequiredUIAccess => RequiresEdt}
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.worksheet.ui.printers.WorksheetEditorPrinterFactory

import java.awt.event.{MouseAdapter, MouseEvent}
import java.awt.geom.Path2D
import java.awt.{BorderLayout, Color, Component, Container, Graphics, Graphics2D, GridBagLayout, RenderingHints}
import javax.swing.{JComponent, JLayeredPane}

//noinspection ApiStatus
object WorksheetDiffSplitters {
  private val Log = Logger.getInstance(this.getClass)

  private val COLOR1 = JBColor.GRAY
  private val COLOR2 = JBColor.LIGHT_GRAY

  class SimpleWorksheetSplitter private[WorksheetDiffSplitters](
    editor1: Editor,
    editor2: Editor,
    private var mappings: Iterable[DiffMapping],
    prop: Float
  ) extends Splitter (false, prop) {

    private val visibleAreaListener: VisibleAreaListener = (_: VisibleAreaEvent) => redrawDiffs()

    // TODO: improve the way how rendered split diff poligons is tested
    @TestOnly
    def renderedPolygons: Option[Seq[DividerPolygon]] = _renderedPolygons
    private var _renderedPolygons: Option[Seq[DividerPolygon]] = None
    private val isUnitTestMode = ApplicationManager.getApplication.isUnitTestMode

    init()

    private def init(): Unit = {
      setDividerWidth(30)
      setFirstComponent(editor1.getComponent)
      setSecondComponent(editor2.getComponent)
      setHonorComponentsMinimumSize(false)

      getDivider.addMouseListener(new MouseAdapter {
        override def mouseReleased(mouseEvent: MouseEvent): Unit = {
          val documentManager = PsiDocumentManager.getInstance(editor1.getProject)
          val file = documentManager.getCachedPsiFile(editor1.getDocument)
          file match {
            case file: ScalaFile =>
              WorksheetEditorPrinterFactory.saveOnlyRatio(file.getVirtualFile, getProportion)
            case _ =>
          }
        }
      })

      editor1.getScrollingModel.addVisibleAreaListener(visibleAreaListener)
    }

    def update(newMappings: Iterable[DiffMapping]): Unit = {
      mappings = newMappings
      redrawDiffs()
    }

    def clear(): Unit = update(Seq())

    def redrawDiffs(): Unit =
      if (!isUnitTestMode) {
        getDivider.repaint()
      } else {
        val polygons = getDivider.asInstanceOf[SimpleWorksheetDivider].generatePolygons
        _renderedPolygons = Some(polygons.toArray.toSeq)
      }

    override def createDivider(): Divider = new SimpleWorksheetDivider()

    private class SimpleWorksheetDivider extends DividerImpl {

      override def paint(g: Graphics): Unit = {
        super.paint(g)
        if (mappings.isEmpty) return

        val width = getWidth
        val height = getHeight
        val editorHeight = editor1.getComponent.getHeight

        val gg = g.create(0, height - editorHeight, width, editorHeight).asInstanceOf[Graphics2D]
        val rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        gg.setRenderingHints(rh)

        val polygons = generatePolygons

        for (polygon <- polygons) {
          polygon.paint(gg, width)
        }

        gg.dispose()
      }

      def generatePolygons: Iterable[DividerPolygon] = {
        // returns mappings of left lines to right lines
        def diffToLinesMapping(diff: DiffMapping): (Segment, Segment) = {
          val DiffMapping(from, to, offset, spaces) = diff
          val start1 = from
          val end1   = to + 1
          val start2 = offset - (to - from)
          val end2   = offset + spaces + 1
          Segment(start1, end1) -> Segment(start2, end2)
        }

        val lineHeight1 = editor1.getLineHeight
        val lineHeight2 = editor2.getLineHeight

        // returns mappings of left offsets to right offsets (in pixels)
        // (relative to the beginning of the document, not screen)
        def linesToOffsetMapping(tuple: (Segment, Segment)): (Segment, Segment) =
          tuple._1 * lineHeight1 -> tuple._2 * lineHeight2

        // ranges for both editors can differ if  font sizes differ, but usually are equal cause font size is the same
        val visibleInterval1 = calcVisibleInterval(editor1)
        val visibleInterval2 = calcVisibleInterval(editor2)

        val offsetMappings = mappings.map(diffToLinesMapping).map(linesToOffsetMapping)
        val plainPolygons = offsetMappings.zipWithIndex.collect {
          case ((seg1, seg2), idx)
            if visibleInterval1.intersects(seg1) || visibleInterval2.intersects(seg2) =>

            // relative to the beginning of the editor component window top
            val start1 = seg1.start - visibleInterval1.start
            val end1   = seg1.end - visibleInterval1.start
            val start2 = seg2.start - visibleInterval2.start
            val end2   = seg2.end - visibleInterval2.start

            val isEven = (idx & 1) == 0
            // to switch color between sibling polygons, to make them more noticeable
            val fillColor = if (isEven) COLOR1 else COLOR2

            new DividerPolygon(start1, start2, end1, end2, fillColor)
        }
        plainPolygons
      }
    }

    /**
     * @return first and last visible line offsets in pixels (from top of the editor)
     *         visible means that it it doesnt care about folded lines
     */
    private def calcVisibleInterval(editor: Editor): Segment = {
      if (!isUnitTestMode) {
        val verticalScrollOffset = editor.getScrollingModel.getVerticalScrollOffset
        val editorHeight = editor.getComponent.getHeight
        val first = verticalScrollOffset
        val last = verticalScrollOffset + editorHeight
        Segment(first, last)
      } else {
        Segment(0, 100500)
      }
    }
  }

  /**
   * upstream: com.intellij.diff.util.DiffDividerDrawUtil.DividerPolygon (internal in Consulo), painted with curved
   * borders
   */
  final class DividerPolygon(val start1: Int, val start2: Int, val end1: Int, val end2: Int, val fillColor: Color) {

    def paint(g: Graphics2D, width: Int): Unit = {
      val path = new Path2D.Double()
      val middle = width / 2.0
      path.moveTo(0, start1)
      path.curveTo(middle, start1, middle, start2, width, start2)
      path.lineTo(width, end2)
      path.curveTo(middle, end2, middle, end1, 0, end1)
      path.closePath()
      g.setColor(fillColor)
      g.fill(path)
    }

    override def toString: String = s"DividerPolygon($start1, $start2, $end1, $end2)"
  }

  /**
   * @param leftStartLine       (inclusive)
   * @param leftEndLine         (inclusive)
   * @param rightFoldLineOffset (visible)
   */
  case class DiffMapping(leftStartLine: Int,
                         leftEndLine: Int,
                         rightFoldLineOffset: Int,
                         rightFoldedLinesCount: Int)

  /**
   * @param start (inclusive)
   * @param end   (inclusive)
   */
  private case class Segment(start: Int, end: Int) {
    def intersects(other: Segment): Boolean = start <= other.end && other.start <= end
    def * (a: Int): Segment = Segment(start * a, end * a)
  }

  private val DiffSplitterKey: Key[SimpleWorksheetSplitter] = Key.create("SimpleWorksheetViewerSplitter")

  def getSplitter(editor: Editor): Option[SimpleWorksheetSplitter] = Option(editor.getUserData(DiffSplitterKey))

  @RequiresEdt
  def addSplitterIfNeeded(
    editor: Editor,
    viewer: Editor,
    proportion: Float,
  ): Unit = {
    val existingSplitter = editor.getUserData(DiffSplitterKey)

    //There can already be a splitter, in this case just do nothing.
    //This can happen e.g. if you 1. run worksheet 2. close tab 3. open the tab back 4. evaluate the worksheet
    //This method will be called 2 times: first time to restore the editor (splitter will also be restored),
    // second time to initiate UI during first worksheet evaluation
    //Another use case is when you change worksheet run type. In this case new UI initialization will be invoked
    if (existingSplitter == null) {
      val splitter = addSplitter(editor, viewer, proportion)
      editor.putUserData(DiffSplitterKey, splitter)
    }
  }

  private def addSplitter(
    editor: Editor,
    viewer: Editor,
    proportion: Float,
  ): SimpleWorksheetSplitter = {
    val editorComponent: JComponent = editor.getComponent
    val editorContentComponent: JComponent = editor.getContentComponent

    // upstream: the parent is com.intellij.openapi.fileEditor.impl.text.TextEditorComponent (internal in Consulo)
    val textEditorComponent = editorComponent.getParent match {
      case null =>
        throw new IllegalStateException("Editor component has no parent")
      case parent => parent
    }

    //NOTE: we need to get constraint before creating splitter, because latter will modify the UI structure
    val constraint = getConstraint(textEditorComponent, editorComponent)

    //NOTE: `createSimpleSplitter` deletes `editorComponent` from `editorComponentParent` children
    val splitter = new SimpleWorksheetSplitter(editor, viewer, Nil, proportion)

    preserveFocus(editorContentComponent) {
      textEditorComponent.remove(editorComponent) //NOTE: seems like no need to do this, after we create splitter this component is already deleted
      textEditorComponent.add(splitter, constraint)
      textEditorComponent.revalidate()
    }

    splitter
  }

  private def preserveFocus(component: Component)(body: => Unit): Unit = {
    val hadFocus = component.hasFocus

    body

    if (hadFocus) {
      component.requestFocusInWindow()
    }
  }

  /**
   * Sometimes text editor component can have some extra non-content layers, which we are not interested in
   * For example during IDEA open, when editor is loaded, an extra special `coverComponent` component is added with non-default layer
   * @see SCL-21282
   */
  private def getConstraint(textEditorComponent: Container, component: Component): AnyRef = {
    val componentsAll = textEditorComponent.getComponents
    val componentsFromDefaultLayer = textEditorComponent match {
      case layered: JLayeredPane => componentsAll.filter(c => layered.getLayer(c) == JLayeredPane.DEFAULT_LAYER)
      case _                     => componentsAll
    }
    if (componentsFromDefaultLayer.length != 1) {
      Log.debug(
        s"""Expected 1 child component in text editor component but got ${componentsFromDefaultLayer.length}
           |Parent component:
           |${textEditorComponent.getClass.getName}
           |Children components:
           |${componentsFromDefaultLayer.map(_.getClass.getName).mkString("\n")}""".stripMargin
      )
    }
    textEditorComponent.getLayout match {
      case layout: GridBagLayout => layout.getConstraints(component)
      case layout: BorderLayout  => Option(layout.getConstraints(component)).getOrElse(BorderLayout.CENTER)
      case _                     => null
    }
  }

  @RequiresEdt
  def removeSplitter(
    editor: Editor
  ): Unit = {
    val editorComponent = editor.getComponent
    val splitter = editorComponent.getParent match {
      case s: SimpleWorksheetSplitter => s
      case _ =>
        return
    }
    val textEditorComponent = splitter.getParent match {
      case null =>
        //generally not expected, but could be in some debugging use-cases
        return
      case parent => parent
    }

    val constraint = getConstraint(textEditorComponent, splitter)
    textEditorComponent.remove(splitter)
    textEditorComponent.add(editorComponent, constraint)
    textEditorComponent.revalidate()
    editor.putUserData(DiffSplitterKey, null)
  }
}
