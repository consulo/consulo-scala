package org.jetbrains.plugins.scala.testingSupport.util.scalatest;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.search.PsiShortNamesCache;
import consulo.project.DumbService;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.execution.ui.console.HyperlinkInfoFactory;
import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.ui.color.ColorValue;
import consulo.ui.style.ComponentColors;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * A custom console output filter to build hyperlinks to exact test failure location in ScalaTest.<br>
 * Pretty much copies {@link com.intellij.execution.filters.ExceptionFilter}.
 */
public class ScalaTestFailureLocationFilter implements Filter {

  private final Project myProject;
  private final GlobalSearchScope mySearchScope;

//  private final static Pattern fqnPattern = Pattern.compile("(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

  //See org.jetbrains.plugins.scala.testingSupport.scalaTest.ScalaTestReporterWithLocation.apply
  private final static String SCALA_TEST_FAILURE_LOCATION = "ScalaTestFailureLocation: ";

  public ScalaTestFailureLocationFilter(final Project project, final GlobalSearchScope scope) {
    myProject = project;
    mySearchScope = scope;
  }

  @Nullable
  @Override
  public Result applyFilter(String line, int entireLength) {
    //Examples of the line we are looking for:
    //   ScalaTestFailureLocation: org.example.MyTest1 at (MyTest1.scala:31)
    //   ScalaTestFailureLocation: bug.TestSpec at (TestSpec.scala:11)
    //   ScalaTestFailureLocation: org.scalatest.matchers.MatchersHelper$ at (TestSpec.scala:8)
    if (!line.startsWith(SCALA_TEST_FAILURE_LOCATION))
      return null;

    final int atKeywordLocation = line.indexOf(" at ");
    if (atKeywordLocation < 0) return null;
    //get class name and make sure it is a fqn
//    if (!fqnPattern.matcher(className).matches()) return null;
    int lparenthIndex = line.lastIndexOf("(");
    int rparenthIndex = line.lastIndexOf(")");
    if (lparenthIndex < 0 || rparenthIndex < 0) return null;
    String fileAndLine = line.substring(lparenthIndex + 1, rparenthIndex);
    int colonIndex = fileAndLine.lastIndexOf(":");
    if (colonIndex < 0) return null;
    int lineNumber = ScalaTestFailureLocationFilter.getLineNumber(fileAndLine.substring(colonIndex + 1));
    if (lineNumber < 0) return null;
    final var fileName = fileAndLine.substring(0, colonIndex).trim();

    //NOTE: class name can be empty (see examples in SCL-21627 and https://github.com/scalatest/scalatest/issues/2286)
    //in this case only file name will be used during resolution under the hood
    //
    //Example: `ScalaTestFailureLocation: org.example.MyTest1 at (MyTest1.scala:31)` -> `org.example.MyTest1`
    int classNameStartIndex = SCALA_TEST_FAILURE_LOCATION.length();
    int classNameEndIndex = atKeywordLocation;
    final String className = classNameEndIndex > classNameStartIndex
      ? line.substring(classNameStartIndex, classNameEndIndex).trim()
      : "";

    final var resolutionResult = resolveClassOrFile(className, fileName);

    final List<VirtualFile> virtualFiles = new ArrayList<>(resolutionResult.classes().keySet());
    if (virtualFiles.isEmpty())
      return null;

    final int textStartOffset = entireLength - line.length();

    final int highlightStartOffset = textStartOffset + lparenthIndex + 1;
    final int highlightEndOffset = textStartOffset + rparenthIndex;

    TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES);
    if (resolutionResult.inLibrary()) {
      ColorValue libTextColor = ComponentColors.DISABLED_TEXT;
      attributes = attributes.clone();
      attributes.setForegroundColor(libTextColor);
      attributes.setEffectColor(libTextColor);
    }
    HyperlinkInfo linkInfo = HyperlinkInfoFactory.getInstance().createMultipleFilesHyperlinkInfo(virtualFiles, lineNumber - 1, myProject);
    return new Filter.Result(highlightStartOffset, highlightEndOffset, linkInfo, attributes);
  }

  // upstream: ExceptionInfoCache.resolveClassOrFile, not public in Consulo
  private ClassResolveInfo resolveClassOrFile(String className, String fileName) {
    ClassResolveInfo info = resolveClass(className);
    if (info.classes().isEmpty() && fileName != null) {
      PsiFile[] files = PsiShortNamesCache.getInstance(myProject).getFilesByName(fileName);
      info = ClassResolveInfo.create(myProject, files);
    }
    return info;
  }

  private ClassResolveInfo resolveClass(String className) {
    if (className.isEmpty() || DumbService.isDumb(myProject)) {
      return new ClassResolveInfo(Map.of(), false);
    }

    PsiClass[] classes = findClassesPreferringMyScope(className);
    if (classes.length == 0) {
      final int dollarIndex = className.indexOf('$');
      if (dollarIndex >= 0) {
        classes = findClassesPreferringMyScope(className.substring(0, dollarIndex));
      }
    }
    return ClassResolveInfo.create(myProject, classes);
  }

  private PsiClass[] findClassesPreferringMyScope(String className) {
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(myProject);
    PsiClass[] result = psiFacade.findClasses(className, mySearchScope);
    return result.length != 0 ? result : psiFacade.findClasses(className, GlobalSearchScope.allScope(myProject));
  }

  private record ClassResolveInfo(Map<VirtualFile, PsiElement> classes, boolean inLibrary) {
    static ClassResolveInfo create(Project project, PsiElement[] elements) {
      ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
      Map<VirtualFile, PsiElement> result = new LinkedHashMap<>();
      boolean library = true;
      for (PsiElement element : elements) {
        element = element.getNavigationElement();
        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        if (virtualFile == null) continue;
        if (index.isInContent(virtualFile)) {
          if (library) {
            library = false;
            result.clear();
          }
        }
        else if (!library) {
          continue;
        }
        result.put(virtualFile, element);
      }
      return new ClassResolveInfo(result, library);
    }
  }

  private static int getLineNumber(String lineString) {
    // some quick checks to avoid costly exceptions
    if (lineString.isEmpty() || lineString.length() > 9 || !Character.isDigit(lineString.charAt(0))) {
      return -1;
    }

    try {
      return Integer.parseInt(lineString);
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
