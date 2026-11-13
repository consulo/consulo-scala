package org.jetbrains.plugins.scala.lang.refactoring.changeSignature.changeInfo;

import com.intellij.java.impl.refactoring.changeSignature.JavaChangeInfo;
import com.intellij.java.impl.refactoring.changeSignature.JavaParameterInfo;
import org.jetbrains.plugins.scala.lang.refactoring.changeSignature.ScalaParameterInfo;

abstract class ScalaChangeInfoBase implements JavaChangeInfo {
  private ScalaParameterInfo[] myNewParams;

  ScalaChangeInfoBase(ScalaParameterInfo[] newParams) {
    myNewParams = newParams;
  }

  @Override
  public JavaParameterInfo[] getNewParameters() {
    return myNewParams;
  }
}
