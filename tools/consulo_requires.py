#!/usr/bin/env python3
"""
Computes Consulo JPMS modules (and maven artifacts) needed by imports of a ported module.
Index: exported packages of module-info.java of Consulo platform and plugins -> (module name, groupId:artifactId).

usage: consulo_requires.py <consulo-repositories-dir> <module-dir>
prints '--dep' and '--requires' arguments for new_module.py, and unresolved import packages to stderr
"""
import collections
import os
import re
import sys

ROOTS = ['consulo', 'consulo-java', 'consulo-xml', 'consulo-properties', 'consulo-junit', 'consulo-spellchecker',
         'consulo-regexp', 'consulo-maven', 'consulo-copyright', 'consulo-markdown', 'consulo-intellilang', 'consulo-git']


def pom_coordinates(pom):
    text = open(pom, encoding='utf-8').read()
    # project coordinates: ignore nested sections which have their own artifactIds
    for section in ('parent', 'build', 'dependencies', 'dependencyManagement', 'repositories', 'pluginRepositories', 'profiles', 'reporting'):
        text = re.sub(r'(?s)<' + section + r'>.*?</' + section + r'>', '', text)
    group = re.search(r'<groupId>([^<]+)</groupId>', text)
    artifact = re.search(r'<artifactId>([^<]+)</artifactId>', text)
    return (group.group(1) if group else None), artifact.group(1)


def index(base):
    result = {}
    for r in ROOTS:
        for d, dirs, files in os.walk(os.path.join(base, r)):
            dirs[:] = [x for x in dirs if x not in ('target', 'test', 'testData', 'testdata', '.git')]
            if 'module-info.java' not in files or not d.endswith('src/main/java'):
                continue
            text = open(os.path.join(d, 'module-info.java'), encoding='utf-8').read()
            name = re.search(r'module\s+([\w.]+)', text).group(1)
            module_dir = d[:-len('/src/main/java')]
            pom = os.path.join(module_dir, 'pom.xml')
            if not os.path.exists(pom):
                continue
            _, artifact = pom_coordinates(pom)
            # platform artifacts are published as consulo:*, plugin artifacts as consulo.plugin:*
            group = 'consulo' if r == 'consulo' else 'consulo.plugin'
            for pkg in re.findall(r'^\s*exports\s+([\w.]+)\s*;', text, re.M):
                result.setdefault(pkg, (name, group, artifact))
    return result


def main():
    base, module = sys.argv[1], sys.argv[2]
    idx = index(base)
    needed = collections.OrderedDict()
    unresolved = collections.Counter()
    for d, _, files in os.walk(os.path.join(module, 'src/main')):
        for f in files:
            if not f.endswith(('.scala', '.java')) or f == 'module-info.java':
                continue
            text = open(os.path.join(d, f), encoding='utf-8', errors='ignore').read()
            for path in re.findall(r'^\s*import\s+(?:static\s+)?(?:_root_\.)?((?:consulo|com\.intellij|org\.intellij|org\.jetbrains\.(?:idea|jps))\.[\w.]+)', text, re.M):
                parts = path.split('.')
                pkg = None
                for k in range(len(parts), 0, -1):
                    if '.'.join(parts[:k]) in idx:
                        pkg = '.'.join(parts[:k])
                        break
                if pkg:
                    needed.setdefault(idx[pkg], None)
                else:
                    unresolved['.'.join(p for p in parts if not p[:1].isupper())] += 1
    for name, group, artifact in needed:
        g = '${project.groupId}' if group == 'consulo.plugin' else group
        print(f"--dep '{g}:{artifact}:${{project.version}}:provided' --requires {name}")
    for pkg, count in unresolved.most_common():
        print(f'unresolved {count:5d} {pkg}', file=sys.stderr)


if __name__ == '__main__':
    main()
