#!/usr/bin/env python3
"""
Replaces the section of one jar in the Consulo plugin indexes (`<plugin>/lib/index.txt`, read by the plugin class
loader, and `<plugin>/index.txt`: `#<jar name>` followed by the jar's file entries) after the jar was replaced, so the
plugin class loader finds new classes.

usage: reindex_plugin_jar.py <plugin-dir> <jar-name>
"""
import os
import sys
import zipfile


def main():
    plugin_dir, jar_name = sys.argv[1], sys.argv[2]
    with zipfile.ZipFile(os.path.join(plugin_dir, 'lib', jar_name)) as jar:
        entries = [n for n in jar.namelist() if not n.endswith('/')]
    for index in (os.path.join(plugin_dir, 'lib', 'index.txt'), os.path.join(plugin_dir, 'index.txt')):
        if os.path.exists(index):
            reindex(index, jar_name, entries)


def reindex(index, jar_name, entries):
    out, skip, found = [], False, False
    for line in open(index, encoding='utf-8').read().split('\n'):
        if line.startswith('#'):
            skip = line[1:] == jar_name
            if skip:
                found = True
                out.append(line)
                out.extend(entries)
                continue
        if not skip:
            out.append(line)
    if not found:
        out[-1:-1] = ['#' + jar_name] + entries
    open(index, 'w', encoding='utf-8').write('\n'.join(out))


if __name__ == '__main__':
    main()
