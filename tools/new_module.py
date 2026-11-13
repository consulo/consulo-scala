#!/usr/bin/env python3
"""
Generates pom.xml and src/main/java/module-info.java for a ported module.
Exports are computed from packages of module sources (src/main/scala, src/main/java).

usage: new_module.py <module dir> --artifact <suffix> --jpms <module name>
         [--scala] [--dep group:artifact:version[:scope]]... [--requires name]... [--bundle group:artifact]
         [--generate goal]...

  --scala    compile with scala-maven-plugin (and binding scalac plugin)
  --bundle   unpack classes of dependency into this module (for jars without valid JPMS name / split packages)
  --generate maven-consulo-plugin generate goal (generate-icon, generate-localize, ...)
  --automatic no module-info.java, jar gets Automatic-Module-Name (for code running outside IDE, e.g. in user test JVM,
              compiled against libraries without valid JPMS names)
"""
import argparse
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)

LICENSE = '''<?xml version="1.0" encoding="UTF-8"?>
<!--
  - Copyright 2013-2026 consulo.io
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  - http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->
'''


def dependency(spec):
    parts = spec.split(':')
    group, artifact, version = parts[:3]
    scope = parts[3] if len(parts) > 3 else None
    lines = ['        <dependency>', f'            <groupId>{group}</groupId>', f'            <artifactId>{artifact}</artifactId>',
             f'            <version>{version}</version>']
    if scope:
        lines.append(f'            <scope>{scope}</scope>')
    lines.append('        </dependency>')
    return '\n'.join(lines)


def packages(module_dir):
    result = set()
    for root in ('src/main/scala', 'src/main/java'):
        for d, _, files in os.walk(os.path.join(module_dir, root)):
            for f in files:
                if f.endswith(('.scala', '.java')) and f != 'module-info.java':
                    text = open(os.path.join(d, f), encoding='utf-8', errors='ignore').read()
                    clauses = []
                    for line in text.split('\n'):
                        s = line.strip()
                        if not s or s.startswith(('//', '/*', '*')):
                            continue
                        m = re.match(r'package\s+object\s+(\w+)', s)
                        if m:
                            clauses.append(m.group(1))
                            break
                        m = re.match(r'package\s+([\w.]+)\s*;?\s*$', s)
                        if m:
                            clauses.append(m.group(1))
                            continue
                        break
                    package_object = re.search(r'^package\s+object\s+(\w+)', text, re.M)
                    if package_object and package_object.group(1) not in clauses:
                        # package object may follow imports - it belongs to package <clauses>.<object name>
                        clauses.append(package_object.group(1))
                    if clauses:
                        result.add('.'.join(clauses))
    return sorted(result)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('module')
    p.add_argument('--artifact', required=True)
    p.add_argument('--jpms', required=True)
    p.add_argument('--scala', action='store_true')
    p.add_argument('--dep', action='append', default=[])
    p.add_argument('--requires', action='append', default=[])
    p.add_argument('--bundle', action='append', default=[])
    p.add_argument('--generate', action='append', default=[])
    p.add_argument('--automatic', action='store_true')
    p.add_argument('--java-level', help='bytecode level (default.java.level), e.g. 8 for code running in user JVM')
    a = p.parse_args()

    depth = len(os.path.normpath(a.module).split(os.sep))
    plugins = []
    if a.scala:
        plugins.append('''            <plugin>
                <groupId>net.alchim31.maven</groupId>
                <artifactId>scala-maven-plugin</artifactId>
            </plugin>''')
    if a.generate:
        goals = '\n'.join(f'                            <goal>{g}</goal>' for g in a.generate)
        plugins.append(f'''            <plugin>
                <groupId>consulo.maven</groupId>
                <artifactId>maven-consulo-plugin</artifactId>
                <extensions>true</extensions>
                <executions>
                    <execution>
                        <id>gen</id>
                        <phase>generate-sources</phase>
                        <goals>
{goals}
                        </goals>
                    </execution>
                </executions>
            </plugin>''')
    for b in a.bundle:
        group, artifact = b.split(':')
        plugins.append(f'''            <!-- {artifact} classes are bundled into this module (no valid JPMS module name, or split package) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>bundle-{artifact}</id>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>unpack-dependencies</goal>
                        </goals>
                        <configuration>
                            <includeGroupIds>{group}</includeGroupIds>
                            <includeArtifactIds>{artifact}</includeArtifactIds>
                            <includes>**/*.class</includes>
                            <outputDirectory>${{project.build.outputDirectory}}</outputDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>''')
    if a.automatic:
        plugins.append(f'''            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <Automatic-Module-Name>{a.jpms}</Automatic-Module-Name>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>''')
    build = ''
    if plugins:
        build = '\n    <build>\n        <plugins>\n' + '\n'.join(plugins) + '\n        </plugins>\n    </build>\n'
    properties = ''
    if a.java_level:
        properties = f'''
    <properties>
        <!-- runs outside IDE process (user JVM) - upstream: withOutOfIDEAProcessSettings -->
        <default.java.level>{a.java_level}</default.java.level>
    </properties>
'''
    pom = LICENSE + f'''<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>consulo.plugin</groupId>
        <artifactId>org.intellij.scala.parent</artifactId>
        <version>3-SNAPSHOT</version>
        <relativePath>{'../' * depth}pom.xml</relativePath>
    </parent>

    <artifactId>org.intellij.scala-{a.artifact}</artifactId>
    <packaging>jar</packaging>
{properties}{build}
    <dependencies>
''' + '\n'.join(dependency(d) for d in a.dep) + '''
    </dependencies>
</project>
'''
    open(os.path.join(a.module, 'pom.xml'), 'w').write(pom)

    if a.automatic:
        print('generated', a.module, '(automatic module)')
        return

    requires = '\n'.join(f'    requires {r};' if r else '' for r in a.requires)
    exports = '\n'.join(f'    exports {p};' for p in packages(a.module))
    # @NullMarked only for modules reading consulo.annotation (jspecify is transitive there);
    # standalone runtime modules (runners) must not depend on Consulo
    null_marked = any(r.split()[-1] == 'consulo.annotation' for r in a.requires if r)
    header = 'import org.jspecify.annotations.NullMarked;\n\n' if null_marked else ''
    info = header + f'''/**
 * @author VISTALL
 * @since 2026-11-13
 */
''' + ('@NullMarked\n' if null_marked else '') + f'''module {a.jpms} {{
{requires}

{exports}
}}
'''
    os.makedirs(os.path.join(a.module, 'src/main/java'), exist_ok=True)
    open(os.path.join(a.module, 'src/main/java/module-info.java'), 'w').write(info)
    print('generated', a.module)


if __name__ == '__main__':
    main()
