package org.jetbrains.plugins.scala.project;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.ui.PathEditor;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MyPathEditor extends PathEditor {
  public MyPathEditor(FileChooserDescriptor descriptor) {
    super(descriptor);
  }

  public String[] getPaths() {
    return virtualFilesToPaths(getRoots());
  }

  public void setPaths(String[] paths) {
    resetPath(pathsToVirtualFiles(paths));
  }

  private static List<VirtualFile> pathsToVirtualFiles(String[] urls) {
    List<VirtualFile> result = new ArrayList<VirtualFile>(urls.length);
    for (String url : urls) {
      result.add(pathToVirtualFile(url));
    }
    return result;
  }

  static VirtualFile pathToVirtualFile(String url) {
    String path = VirtualFileUtil.urlToPath(url);
    VirtualFile file = VirtualFileUtil.findFileByIoFile(Path.of(path).toFile(), true);
    return file == null ? new AbsentLocalFile(url, path) : file;
  }

  private static String[] virtualFilesToPaths(VirtualFile[] files) {
    String[] result = new String[files.length];
    int i = 0;
    for (VirtualFile file : files) {
      result[i] = file.getUrl();
      i++;
    }
    return result;
  }
}
