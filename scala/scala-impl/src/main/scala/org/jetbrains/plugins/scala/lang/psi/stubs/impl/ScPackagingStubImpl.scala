package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.stub.StubBase
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging
import org.jetbrains.plugins.scala.lang.psi.stubs.{RawStubElement, ScPackagingStub}

final class ScPackagingStubImpl(parent: RawStubElement,
                                elementType: IStubElementType[?, ?],
                                override val packageName: String,
                                override val parentPackageName: String,
                                override val isExplicit: Boolean)
  extends StubBase[ScPackaging](parent, elementType) with ScPackagingStub
