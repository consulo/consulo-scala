package org.jetbrains.plugins.scala.debugger.filters;

import consulo.annotation.component.ExtensionImpl;

import consulo.configurable.Configurable;
import com.intellij.openapi.options.SimpleConfigurable;
import consulo.util.xml.serializer.XmlSerializerUtil;
import com.intellij.xdebugger.settings.DebuggerSettingsCategory;
import com.intellij.xdebugger.settings.XDebuggerSettings;
import org.jetbrains.plugins.scala.debugger.DebuggerBundle;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

@ExtensionImpl
public class ScalaDebuggerSettings extends XDebuggerSettings<ScalaDebuggerSettings> implements Supplier<ScalaDebuggerSettings> {

  public boolean DEBUG_DISABLE_SPECIFIC_SCALA_METHODS = true;
  public boolean FRIENDLY_COLLECTION_DISPLAY_ENABLED = true;
  public boolean DONT_SHOW_RUNTIME_REFS = true;
  public boolean SHOW_VARIABLES_FROM_OUTER_SCOPES = true;
  public boolean ALWAYS_SMART_STEP_INTO = true;

  public ScalaDebuggerSettings() {
    super("scala_debugger");
  }

  @Override
  public Collection<? extends Configurable> createConfigurables(DebuggerSettingsCategory category) {
    //todo: split settings configurables somehow
    return switch (category) {
      case GENERAL -> singletonList(new ScalaDebuggerSettingsConfigurable(this));
      case STEPPING -> singletonList(SimpleConfigurable.create("scala.debugger.stepping", DebuggerBundle.message("scala.debug.caption"), ScalaSteppingConfigurable.class, this));
      default -> Collections.emptyList();
    };
  }

  @Override
  public ScalaDebuggerSettings getState() {
    return this;
  }

  @Override
  public void loadState(final ScalaDebuggerSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public static ScalaDebuggerSettings getInstance() {
    return getInstance(ScalaDebuggerSettings.class);
  }

  @Override
  public ScalaDebuggerSettings get() {
    return this;
  }
}