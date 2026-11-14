#!/usr/bin/env python3
"""
Generates pom.xml and module-info.java for a ported module that depends on scala-impl (and optionally other ported
modules): provided dependencies and requires are taken from scala/codeInsight, the module is registered in the root pom
and in the plugin module (packaging).

usage: setup_dependent_module.py <module dir> --artifact <suffix> --jpms <module name> [--needs <artifact suffix>:<jpms>]...
         [--dep group:artifact:version (provided)]... [--requires <jpms>]... [--plugin <plugin id>]...

  --plugin  the module is loaded only when that plugin is present: META-INF/plugin-requires.xml in the module jar and an
            optional <depends> in plugin.xml
"""
import argparse
import os
import re
import subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('module')
    p.add_argument('--artifact', required=True)
    p.add_argument('--jpms', required=True)
    p.add_argument('--needs', action='append', default=[])
    p.add_argument('--dep', action='append', default=[])
    p.add_argument('--requires', action='append', default=[])
    p.add_argument('--plugin', action='append', default=[])
    a = p.parse_args()
    os.chdir(REPO)
    subprocess.check_call(['python3', os.path.join(HERE, 'new_module.py'), a.module, '--artifact', a.artifact, '--jpms', a.jpms,
                           '--scala'], stdout=subprocess.DEVNULL)

    ci = open('scala/codeInsight/pom.xml').read()
    deps = ci[ci.index('<dependencies>') + len('<dependencies>'):ci.index('</dependencies>')]
    for need in a.needs:
        artifact = need.split(':')[0]
        deps += f'''    <dependency>
            <groupId>consulo.plugin</groupId>
            <artifactId>org.intellij.scala-{artifact}</artifactId>
            <version>${{project.version}}</version>
            <scope>provided</scope>
        </dependency>
    '''
    for dep in a.dep:
        group, artifact, version = dep.split(':')
        deps += f'''    <dependency>
            <groupId>{group}</groupId>
            <artifactId>{artifact}</artifactId>
            <version>{version}</version>
            <scope>provided</scope>
        </dependency>
    '''
    pom_path = os.path.join(a.module, 'pom.xml')
    pom = open(pom_path).read()
    pom = re.sub(r'(?s)<dependencies>.*</dependencies>', lambda _: '<dependencies>' + deps + '</dependencies>', pom)
    open(pom_path, 'w').write(pom)

    ci_info = open('scala/codeInsight/src/main/java/module-info.java').read()
    requires = [l for l in ci_info.split('\n') if l.strip().startswith('requires')]
    requires += [f'    requires {need.split(":")[1]};' for need in a.needs]
    requires += [f'    requires {r};' for r in a.requires]
    info_path = os.path.join(a.module, 'src/main/java/module-info.java')
    info = open(info_path).read()
    info = re.sub(r'(?m)^\s*requires [^;]+;\n', '', info)
    info = re.sub(r'module ([\w.]+) \{\n', lambda m: '// open: persisted state classes are read and written reflectively by the Consulo XML serializer\n'
                  'open module ' + m.group(1) + ' {\n' + '\n'.join(requires) + '\n\n', info, count=1)
    open(info_path, 'w').write(info)

    root = open('pom.xml').read()
    if f'<module>{a.module}</module>' not in root:
        root = root.replace('        <module>plugin</module>', f'        <module>{a.module}</module>\n        <module>plugin</module>')
        open('pom.xml', 'w').write(root)

    plugin_pom = open('plugin/pom.xml').read()
    if f'<artifactId>org.intellij.scala-{a.artifact}</artifactId>' not in plugin_pom:
        marker = '        <!-- packaging takes only direct dependencies - all modules and runtime libraries must be listed here -->\n'
        plugin_pom = plugin_pom.replace(marker, marker + f'''        <dependency>
            <groupId>${{project.groupId}}</groupId>
            <artifactId>org.intellij.scala-{a.artifact}</artifactId>
            <version>${{project.version}}</version>
        </dependency>
''')
        open('plugin/pom.xml', 'w').write(plugin_pom)

    plugin_info = open('plugin/src/main/java/module-info.java').read()
    # modules of optional plugin dependencies are not loaded without the plugin - nothing may require them
    if not a.plugin and f'requires transitive {a.jpms};' not in plugin_info:
        plugin_info = re.sub(r'\n\}\s*$', f'\n    requires transitive {a.jpms};\n}}\n', plugin_info)
        open('plugin/src/main/java/module-info.java', 'w').write(plugin_info)

    if a.plugin:
        meta = os.path.join(a.module, 'src/main/resources/META-INF')
        os.makedirs(meta, exist_ok=True)
        plugins = ''.join(f'        <plugin>{p}</plugin>\n' for p in a.plugin)
        open(os.path.join(meta, 'plugin-requires.xml'), 'w').write(
            f'<consulo-plugin-requires>\n    <plugins>\n{plugins}    </plugins>\n</consulo-plugin-requires>\n')
        xml_path = 'plugin/src/main/resources/META-INF/plugin.xml'
        xml = open(xml_path).read()
        for plugin in a.plugin:
            if f'>{plugin}</depends>' not in xml:
                last = list(re.finditer(r'\n([ \t]*)<depends[^>]*>[^<]*</depends>', xml))[-1]
                xml = xml[:last.end()] + f'\n{last.group(1)}<depends optional="true">{plugin}</depends>' + xml[last.end():]
        open(xml_path, 'w').write(xml)


if __name__ == '__main__':
    main()
