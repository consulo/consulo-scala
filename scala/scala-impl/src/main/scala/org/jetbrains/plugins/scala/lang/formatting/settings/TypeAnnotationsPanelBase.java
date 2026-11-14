package org.jetbrains.plugins.scala.lang.formatting.settings;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import org.jetbrains.plugins.scala.ScalaBundle;

public abstract class TypeAnnotationsPanelBase extends ScalaCodeStylePanelBase {

    public static final String TAB_TITLE = ScalaBundle.message("type.annotations.panel.title");

    protected final CheckBox myPublicMember;
    protected final CheckBox myProtectedMember;
    protected final CheckBox myPrivateMember;
    protected final CheckBox myFunctionParameter;
    protected final CheckBox myUnderscoerParameter;
    protected final CheckBox myLocalDefinition;
    protected final CheckBox myImplicitModifier;
    protected final CheckBox myConstant;
    protected final CheckBox myMemberOfAnonymousClass;
    protected final CheckBox myMemberOfPrivateClass;
    protected final CheckBox myDialectSources;
    protected final CheckBox myTestSources;
    protected final CheckBox myUnitType;
    protected final TextBoxWithExpandAction myMembers;
    protected final TextBoxWithExpandAction myAnnotations;
    protected final TextBoxWithExpandAction myTypes;
    protected final Component myContent;
    protected final CheckBox myStableType;
    protected final CheckBox myStructuralType;

    @RequiredUIAccess
    protected TypeAnnotationsPanelBase(CodeStyleSettings settings) {
        super(settings, TAB_TITLE);

        myPublicMember = checkBox("type.annotations.panel.public.member", true);
        myProtectedMember = checkBox("type.annotations.panel.protected.member", true);
        myPrivateMember = checkBox("type.annotations.panel.private.member", false);
        myLocalDefinition = checkBox("type.annotations.panel.local.definition", false);
        myFunctionParameter = checkBox("type.annotations.panel.function.literal.parameter", false);
        myUnderscoerParameter = checkBox("type.annotations.panel.underscore.parameter", false);

        myMemberOfAnonymousClass = checkBox("type.annotations.panel.member.of.anonymous.class", false);
        myMemberOfPrivateClass = checkBox("type.annotations.panel.member.of.private.class", false);
        myConstant = checkBox("type.annotations.panel.constant.final.val", true);
        myStableType = checkBox("type.annotations.panel.type.is.stable", true);
        myTestSources = checkBox("type.annotations.panel.in.test.sources", false);
        myDialectSources = checkBox("type.annotations.panel.in.scala.dialect.sources", false);

        myImplicitModifier = checkBox("type.annotations.panel.implicit.definition", true);
        myUnitType = checkBox("type.annotations.panel.unit.type", true);
        myStructuralType = checkBox("type.annotations.panel.accidental.structural.type", true);

        myMembers = listTextBox("type.annotations.panel.classes");
        myAnnotations = listTextBox("type.annotations.panel.annotations");
        myTypes = listTextBox("type.annotations.panel.type.patterns");

        VerticalLayout useFor = VerticalLayout.create()
                .add(myPublicMember)
                .add(myProtectedMember)
                .add(myPrivateMember)
                .add(myLocalDefinition)
                .add(myFunctionParameter)
                .add(myUnderscoerParameter);

        VerticalLayout exceptWhen = VerticalLayout.create()
                .add(myMemberOfAnonymousClass)
                .add(myMemberOfPrivateClass)
                .add(LabeledBuilder.filled(LocalizeValue.of(ScalaBundle.message("type.annotations.panel.member.of")), myMembers))
                .add(LabeledBuilder.filled(LocalizeValue.of(ScalaBundle.message("type.annotations.panel.annotated.with")), myAnnotations))
                .add(myConstant)
                .add(withContextHelp(myStableType, ScalaBundle.message("type.annotations.panel.type.is.stable.tooltip")))
                .add(LabeledBuilder.filled(LocalizeValue.of(ScalaBundle.message("type.annotations.panel.type.matches")), myTypes))
                .add(myTestSources)
                .add(withContextHelp(myDialectSources, ScalaBundle.message("type.annotations.panel.in.scala.dialect.sources.tooltip")));

        VerticalLayout enforceFor = VerticalLayout.create()
                .add(myImplicitModifier)
                .add(myUnitType)
                .add(withContextHelp(myStructuralType, ScalaBundle.message("type.annotations.panel.accidental.structural.type.tooltip")));

        VerticalLayout content = VerticalLayout.create()
                .add(HorizontalLayout.create(Space.LARGE)
                        .add(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("type.annotations.panel.use.for")), useFor))
                        .add(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("type.annotations.panel.except.when")), exceptWhen)))
                .add(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("type.annotations.panel.enforce.for")), enforceFor));
        content.paddingBuilder().allSet(Space.MEDIUM).apply();
        myContent = content;
    }

    @RequiredUIAccess
    private static CheckBox checkBox(String key, boolean selected) {
        return CheckBox.create(LocalizeValue.of(ScalaBundle.message(key)), selected);
    }

    // upstream: TextFieldWithBrowseButton opening Messages.showTextAreaDialog
    @RequiredUIAccess
    private static TextBoxWithExpandAction listTextBox(String titleKey) {
        return TextBoxWithExpandAction.create(
                PlatformIconGroup.generalExpandcomponent(),
                ScalaBundle.message(titleKey),
                ParametersListUtil.COLON_LINE_PARSER,
                ParametersListUtil.COLON_LINE_JOINER
        );
    }
}
