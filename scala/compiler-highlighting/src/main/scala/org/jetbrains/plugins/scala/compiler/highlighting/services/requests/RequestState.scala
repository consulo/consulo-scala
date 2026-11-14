package org.jetbrains.plugins.scala.compiler.highlighting.services.requests

import consulo.codeEditor.{Editor, EditorFactory}
import consulo.document.Document
import consulo.language.editor.completion.lookup.LookupManager
import consulo.language.editor.template.TemplateManager
import consulo.project.Project

enum RequestState:
  case Ready, NotReady, Expired

object RequestState:
  /**
   * `Expired` when nothing can show the result, `NotReady` while the user is mid-gesture, `Ready`
   * otherwise.
   *
   * A document with no editor is expired rather than delayed: there is nowhere to draw the answer, so
   * waiting for one would mean waiting for ever. An active lookup or live template is the opposite case,
   * a gesture that usually ends without the text changing, so the work is worth keeping and retrying.
   */
  def of(project: Project, document: Document): RequestState =
    val editors = EditorFactory.getInstance().getEditors(document, project)
    if editors.isEmpty then RequestState.Expired
    else if editors.exists(usable(project, _)) then RequestState.Ready
    else RequestState.NotReady

  private def usable(project: Project, editor: Editor): Boolean =
    LookupManager.getActiveLookup(editor) == null &&
      TemplateManager.getInstance(project).getActiveTemplate(editor) == null
