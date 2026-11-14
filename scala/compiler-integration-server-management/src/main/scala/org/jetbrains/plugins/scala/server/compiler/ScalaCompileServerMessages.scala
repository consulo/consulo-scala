package org.jetbrains.plugins.scala.server.compiler
import consulo.component.util.localize.{AbstractBundle => DynamicBundle}
import org.jetbrains.jps.incremental.scala.shared.ScalaCompileServerMessagesShared

//duplicated in org.jetbrains.jps.incremental.scala.ScalaCompileServerJpsBundle (but with JpsDynamicBundle)
object ScalaCompileServerMessages
  extends ScalaCompileServerMessagesShared(new DynamicBundle(classOf[ScalaCompileServerMessages.type], ScalaCompileServerMessagesShared.BUNDLE))
