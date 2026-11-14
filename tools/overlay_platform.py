#!/usr/bin/env python3
"""
Overlays locally built Consulo platform module jars (changed for the port, not yet in a hub build) onto the platform
builds downloaded into the sandbox: every artifact listed in tools/platform_overlay.txt (one maven artifactId per line,
e.g. consulo-execution-debug-api; '#' comments) is copied from ~/.m2/repository/consulo/<artifactId>/3-SNAPSHOT over the
jar of the same name in <sandbox>/platform-*/Consulo/platform/build*/modules/*/lib and the module indexes are updated.

usage: overlay_platform.py <sandbox dir>
"""
import glob
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def main():
    sandbox = sys.argv[1]
    listing = os.path.join(HERE, 'platform_overlay.txt')
    if not os.path.exists(listing):
        return
    artifacts = [line.split('#')[0].strip() for line in open(listing, encoding='utf-8')]
    artifacts = [a for a in artifacts if a]
    module_dirs = glob.glob(os.path.join(sandbox, 'platform-*', 'Consulo', 'platform', 'build*', 'modules', '*'))
    for artifact in artifacts:
        jar_name = artifact + '-3-SNAPSHOT.jar'
        local_jar = os.path.expanduser(os.path.join('~/.m2/repository/consulo', artifact, '3-SNAPSHOT', jar_name))
        if not os.path.exists(local_jar):
            print('overlay_platform: missing ' + local_jar)
            continue
        replaced = False
        for module_dir in module_dirs:
            target = os.path.join(module_dir, 'lib', jar_name)
            if os.path.exists(target):
                shutil.copyfile(local_jar, target)
                subprocess.check_call([sys.executable, os.path.join(HERE, 'reindex_plugin_jar.py'), module_dir, jar_name])
                replaced = True
        if not replaced:
            print('overlay_platform: no platform jar ' + jar_name + ' in the sandbox (new platform module?)')


if __name__ == '__main__':
    main()
