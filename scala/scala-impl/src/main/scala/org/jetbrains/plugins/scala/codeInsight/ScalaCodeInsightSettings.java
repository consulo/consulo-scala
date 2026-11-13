package org.jetbrains.plugins.scala.codeInsight;

import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

@State(
        name = "ScalaCodeInsightSettings",
        storages = {@Storage("scala_code_insight_settings.xml")}
)
public class ScalaCodeInsightSettings implements PersistentStateComponent<ScalaCodeInsightSettings> {

    public static final boolean SHOW_PARAMETER_NAMES_DEFAULT = false;
    public static final boolean SHOW_METHOD_RESULT_TYPE_DEFAULT = true;
    public static final boolean SHOW_MEMBER_VARIABLE_TYPE_DEFAULT = false;
    public static final boolean SHOW_LOCAL_VARIABLE_TYPE_DEFAULT = false;
    public static final boolean SHOW_METHOD_CHAIN_INLAY_HINTS_DEFAULT = true;
    public static final boolean ALIGN_METHOD_CHAIN_INLAY_HINTS_DEFAULT = true;
    public static final int UNIQUE_TYPES_TO_SHOW_METHOD_CHAINS_DEFAULT = 3;
    public static final int PRESENTATION_LENGTH_DEFAULT = 45;
    public static final boolean SHOW_OBVIOUS_TYPE_DEFAULT = false;
    public static final boolean PRESERVE_INDENTS_DEFAULT = true;
    public static final boolean SHOW_RANGE_HINTS_FOR_TO_AND_UNTIL_DEFAULT = true;
    public static final boolean SHOW_EXCLUSIVE_RANGE_HINT_DEFAULT = true;

    public static final int MIN_PRESENTATION_LENGTH = 1;
    public static final int MAX_PRESENTATION_LENGTH = Byte.MAX_VALUE;

    public static ScalaCodeInsightSettings getInstance() {
        return ApplicationManager.getApplication().getInstance(ScalaCodeInsightSettings.class);
    }

    //private fields are not serialized
    public boolean showParameterNames = SHOW_PARAMETER_NAMES_DEFAULT;
    // TODO Rename to "showMethodResultType" (setting format upgrade is required)
    public boolean showFunctionReturnType = SHOW_METHOD_RESULT_TYPE_DEFAULT;
    // TODO Rename to "showMemberVariableType" (setting format upgrade is required)
    public boolean showPropertyType = SHOW_MEMBER_VARIABLE_TYPE_DEFAULT;
    public boolean showLocalVariableType = SHOW_LOCAL_VARIABLE_TYPE_DEFAULT;
    public boolean showMethodChainInlayHints = SHOW_METHOD_CHAIN_INLAY_HINTS_DEFAULT;
    public boolean alignMethodChainInlayHints = ALIGN_METHOD_CHAIN_INLAY_HINTS_DEFAULT;
    public int uniqueTypesToShowMethodChains = UNIQUE_TYPES_TO_SHOW_METHOD_CHAINS_DEFAULT;
    public int presentationLength = PRESENTATION_LENGTH_DEFAULT;
    public boolean showObviousType = SHOW_OBVIOUS_TYPE_DEFAULT;
    public boolean preserveIndents = PRESERVE_INDENTS_DEFAULT;
    public boolean showRangeHintsForToAndUntil = SHOW_RANGE_HINTS_FOR_TO_AND_UNTIL_DEFAULT;
    public boolean showExclusiveRangeHint = SHOW_EXCLUSIVE_RANGE_HINT_DEFAULT;


    public Supplier<Boolean> showParameterNamesGetter() {
        return () -> showParameterNames;
    }

    public Consumer<Boolean> showParameterNamesSetter() {
        return value -> showParameterNames = value;
    }

    public Supplier<Boolean> showMethodResultTypeGetter() {
        return () -> showFunctionReturnType;
    }

    public Consumer<Boolean> showMethodResultTypeSetter() {
        return value -> showFunctionReturnType = value;
    }

    public Supplier<Boolean> showMemberVariableTypeGetter() {
        return () -> showPropertyType;
    }

    public Consumer<Boolean> showMemberVariableSetter() {
        return value -> showPropertyType = value;
    }

    public Supplier<Boolean> showLocalVariableTypeGetter() {
        return () -> showLocalVariableType;
    }

    public Consumer<Boolean> showLocalVariableTypeSetter() {
        return value -> showLocalVariableType = value;
    }

    public Supplier<Boolean> showMethodChainInlayHintsGetter() {
        return () -> showMethodChainInlayHints;
    }

    public Consumer<Boolean> showMethodChainInlayHintsSetter() {
        return value -> showMethodChainInlayHints = value;
    }

    public Supplier<Boolean> alignMethodChainInlayHintsGetter() {
        return () -> alignMethodChainInlayHints;
    }

    public Consumer<Boolean> alignMethodChainInlayHintsSetter() {
        return value -> alignMethodChainInlayHints = value;
    }

    public Supplier<Integer> uniqueTypesToShowMethodChainsGetter() {
        return () -> uniqueTypesToShowMethodChains;
    }

    public Consumer<Integer> uniqueTypesToShowMethodChainsSetter() {
        return value -> uniqueTypesToShowMethodChains = value;
    }

    public Supplier<Integer> presentationLengthGetter() {
        return () -> presentationLength;
    }

    public Consumer<Integer> presentationLengthSetter() {
        return value -> presentationLength = value;
    }

    public Supplier<Boolean> showObviousTypeGetter() {
        return () -> showObviousType;
    }

    public Consumer<Boolean> showObviousTypeSetter() {
        return value -> showObviousType = value;
    }

    public Supplier<Boolean> preserveIndentsGetter() {
        return () -> preserveIndents;
    }

    public Consumer<Boolean> preserveIndentsSetter() {
        return value -> preserveIndents = value;
    }

    public Supplier<Boolean> showRangeHintsForToAndUntilGetter() {
        return () -> showRangeHintsForToAndUntil;
    }

    public Consumer<Boolean> showRangeHintsForToAndUntilSetter() {
        return value -> showRangeHintsForToAndUntil = value;
    }


    public Supplier<Boolean> showExclusiveRangeHintDefaultGetter() {
        return () -> showExclusiveRangeHint;
    }

    public Consumer<Boolean> showExclusiveRangeHintDefaultSetter() {
        return value -> showExclusiveRangeHint = value;
    }

    @Override
    public ScalaCodeInsightSettings getState() {
        return this;
    }

    @Override
    public void loadState(ScalaCodeInsightSettings settings) {
        XmlSerializerUtil.copyBean(settings, this);
    }
}

