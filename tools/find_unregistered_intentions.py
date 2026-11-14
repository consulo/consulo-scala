#!/usr/bin/env python3
"""
Lists concrete IntentionAction classes of a plugin jar without @IntentionMetaData and not SyntheticIntentionAction.
Consulo requires one of them for every intention shown in the popup (IntentionManagerSettings.getIgnoreId), so the
unregistered ones (quick fixes passed to annotations etc.) have to implement SyntheticIntentionAction.

usage: find_unregistered_intentions.py <plugin-jar> <dir-or-jar with platform jars>...
"""
import os
import struct
import sys
import zipfile

INTENTION = 'consulo/language/editor/intention/IntentionAction'
SYNTHETIC = 'consulo/language/editor/intention/SyntheticIntentionAction'
META = 'Lconsulo/language/editor/intention/IntentionMetaData;'
ACC_INTERFACE, ACC_ABSTRACT = 0x0200, 0x0400


def parse(data):
    count = struct.unpack('>H', data[8:10])[0]
    pool, i, idx = {}, 10, 1
    while idx < count:
        tag = data[i]
        if tag == 1:
            n = struct.unpack('>H', data[i + 1:i + 3])[0]
            pool[idx] = ('utf8', data[i + 3:i + 3 + n].decode('utf-8', 'replace'))
            i += 3 + n
        elif tag == 7:
            pool[idx] = ('class', struct.unpack('>H', data[i + 1:i + 3])[0])
            i += 3
        elif tag in (8, 16, 19, 20):
            i += 3
        elif tag == 15:
            i += 4
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            i += 5
        elif tag in (5, 6):
            i += 9
            idx += 1
        else:
            raise ValueError('tag %d' % tag)
        idx += 1

    def cls(k):
        return pool[pool[k][1]][1] if k else None

    access, this, sup, n = struct.unpack('>HHHH', data[i:i + 8])
    interfaces = [cls(struct.unpack('>H', data[i + 8 + 2 * j:i + 10 + 2 * j])[0]) for j in range(n)]
    utf8 = {v[1] for v in pool.values() if v[0] == 'utf8'}
    return cls(this), access, cls(sup), interfaces, META in utf8


def read_jar(path, into, own):
    with zipfile.ZipFile(path) as jar:
        for name in jar.namelist():
            if name.endswith('.class') and not name.endswith('module-info.class'):
                try:
                    this, access, sup, interfaces, meta = parse(jar.read(name))
                except Exception:
                    continue
                into[this] = (access, sup, interfaces, meta)
                if own is not None:
                    own.add(this)


def main():
    classes, own = {}, set()
    for arg in sys.argv[2:]:
        jars = [arg] if arg.endswith('.jar') else [os.path.join(d, f) for d, _, fs in os.walk(arg) for f in fs if f.endswith('.jar')]
        for jar in jars:
            read_jar(jar, classes, None)
    read_jar(sys.argv[1], classes, own)

    memo = {}

    def supertypes(name):
        if name in memo:
            return memo[name]
        memo[name] = set()
        result = {name}
        info = classes.get(name)
        if info:
            for parent in [info[1]] + info[2]:
                if parent:
                    result |= supertypes(parent)
        memo[name] = result
        return result

    for name in sorted(own):
        access, _, _, meta = classes[name]
        if access & (ACC_INTERFACE | ACC_ABSTRACT) or meta:
            continue
        types = supertypes(name)
        if INTENTION in types and SYNTHETIC not in types:
            print(name.replace('/', '.'))


if __name__ == '__main__':
    main()
