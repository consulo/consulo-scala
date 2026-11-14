package org.jetbrains.sbt.project.module

import consulo.application.WriteAction
import consulo.externalSystem.service.module.extension.ExternalSystemMutableModuleExtension
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.module.Module
import consulo.module.content.ModuleRootManager
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.api.resolvers.{SbtIvyResolver, SbtMavenResolver, SbtResolver}
import org.jetbrains.sbt.project.SbtProjectSystem

import java.net.URI
import java.util.regex.Pattern
import scala.jdk.CollectionConverters.*

// upstream: lives in sbt-api
object SbtModule {

  // upstream: a module-level PersistentStateComponent (SbtModuleState) - Consulo has no module-level persistent services,
  // the values are stored as options of the sbt external system module extension
  private val BuildForUriKey = "sbt.build.for.uri"
  private val ImportsKey = "sbt.imports"
  private val ResolversKey = "sbt.resolvers"

  private def getOption(module: Module, key: String): String =
    ExternalSystemApiUtil.getExtensionSystemOption(SbtProjectSystem.Id, module, key)

  private def setOption(module: Module, key: String, value: String): Unit = {
    val model = ModuleRootManager.getInstance(module).getModifiableModel
    val extension: ExternalSystemMutableModuleExtension[?] = model.getExtensionWithoutCheck(SbtProjectSystem.Id.getId)
    if (extension == null) {
      model.dispose()
      return
    }
    extension.setOption(key, value)
    WriteAction.run(() => model.commit())
  }

  object Build {

    def apply(module: Module): URI =
      new URI(Option(getOption(module, BuildForUriKey)).getOrElse(""))

    def update(module: Module, uri: URI): Unit =
      setOption(module, BuildForUriKey, uri.toString)
  }

  object Imports {

    private val ImportsDelimiter = ", "

    // substitution of dollars is necessary because IDEA will interpret a string in the form of $something$ as a path variable
    // and warn the user of "undefined path variables" (SCL-10691)
    private val SubstitutePrefix = "SUB:"
    private val SubstituteDollar = "DOLLAR"

    private def encode(text: String): String = {
      text
        .replace(SubstitutePrefix, SubstitutePrefix + SubstitutePrefix)
        .replace("$", SubstitutePrefix + SubstituteDollar)
    }

    private def decode(text: String): String =
      text
        .replace(SubstitutePrefix + SubstitutePrefix, SubstitutePrefix)
        .replace(SubstitutePrefix + SubstituteDollar, "$")

    def apply(module: Module): Seq[String] = {
      val importsStrOpt = Option(getOption(module, ImportsKey)).filter(_.nonEmpty)
      importsStrOpt.fold(Sbt.DefaultImplicitImports)(deserializeSeq)
    }

    def update(module: Module, imports: java.util.List[String]): Unit = {
      val newImports = serializeSeq(imports.asScala.toSeq)
      setOption(module, ImportsKey, newImports)
    }

    private def serializeSeq(imports: Seq[String]): String = {
      val concatenated = imports.mkString(ImportsDelimiter)
      val encoded = encode(concatenated)
      encoded
    }

    private def deserializeSeq(string: String): Seq[String] = {
      val decoded = decode(string)
      val parts = decoded.split(ImportsDelimiter)
      parts.toSeq
    }
  }

  object Resolvers {

    def apply(module: Module): Set[SbtResolver] = {
      val resolversStrOpt = Option(getOption(module, ResolversKey)).filter(_.nonEmpty)
      resolversStrOpt.toSet.flatMap(deserializeSet)
    }

    def update(module: Module, resolvers: Set[SbtResolver]): Unit = {
      val serialized = serializeSet(resolvers)
      setOption(module, ResolversKey, serialized)
    }

    private val ResolversDelimiter = ", "
    private val ResolverFieldsDelimiter = "|"
    private val ResolverFieldsDelimiterEncoded = "&delim;"
    private val ResolverFieldsDelimiterPattern = Pattern.quote(ResolverFieldsDelimiter)

    private def encodeName(name: String): String = name.replace(ResolverFieldsDelimiter, ResolverFieldsDelimiterEncoded)
    private def decodeName(name: String): String = name.replace(ResolverFieldsDelimiterEncoded, ResolverFieldsDelimiter)

    private def serialize(resolver: SbtResolver): String = {
      val root = resolver.root
      val name = encodeName(resolver.name)
      val parts = resolver match {
        case _: SbtMavenResolver => Seq(root, "maven", name)
        case ir: SbtIvyResolver => Seq(root, "ivy", ir.isLocal, name)
      }
      parts.mkString(ResolverFieldsDelimiter)
    }

    private def deserialize(resolverStr: String): Option[SbtResolver] = {
      val parts = resolverStr.split(ResolverFieldsDelimiterPattern).toSeq
      parts match {
        case Seq(root, "maven", name)        => Some(new SbtMavenResolver(decodeName(name), root))
        case Seq(root, "ivy", isLocal, name) => Some(new SbtIvyResolver(decodeName(name), root, isLocal = isLocal.toBooleanOption.getOrElse(false)))
        case _                               => None
      }
    }

    private def serializeSet(resolvers: Set[SbtResolver]): String =
      resolvers.map(serialize).mkString(ResolversDelimiter)

    private def deserializeSet(string: String): Set[SbtResolver] = {
      val resolvers = string.split(ResolversDelimiter)
      resolvers.flatMap(deserialize).toSet
    }
  }
}
