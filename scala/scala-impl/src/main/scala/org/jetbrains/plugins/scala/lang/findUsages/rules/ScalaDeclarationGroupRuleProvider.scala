package org.jetbrains.plugins.scala.lang.findUsages.rules

import consulo.project.Project
import consulo.usage.rule.FileStructureGroupRuleProvider
import consulo.usage.rule.UsageGroupingRule

final class ScalaDeclarationGroupRuleProvider extends FileStructureGroupRuleProvider {
  override def getUsageGroupingRule(project: Project): UsageGroupingRule =
    new ScalaDeclarationUsageGroupingRule(true)
}

final class ScalaDeclarationSecondLevelGroupRuleProvider extends FileStructureGroupRuleProvider {
  override def getUsageGroupingRule(project: Project): UsageGroupingRule =
    new ScalaDeclarationUsageGroupingRule(false)
}