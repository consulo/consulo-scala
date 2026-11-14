#!/usr/bin/env bash
# Packages the plugin and runs it in a Consulo sandbox (platform and consulo.java from the hub, the version of
# consulo.java matches the downloaded platform build).
# Runs on an isolated X display (Xvfb + openbox), never on the user's display; other sessions use their own displays.
# usage: tools/run_sandbox.sh [display-number]   (default 81)
set -e
REPO=$(cd "$(dirname "$0")/.." && pwd)
DISPLAY_NUM=${1:-81}
MODULE=$REPO/plugin
PLATFORM=${PLATFORM_VERSION:-$(curl -s "https://api.consulo.io/repository/info?id=consulo.dist.linux64&platformVersion=SNAPSHOT&version=SNAPSHOT&channel=nightly" | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])')}

# required plugins (and their required dependencies) for exactly that platform build
DEPS=$MODULE/target/consulo-plugin-dependencies
mkdir -p "$DEPS"
fetch_plugin() {
  local id=$1
  if [ ! -d "$DEPS/$id" ] || [ "$(cat "$DEPS/$id.platform" 2>/dev/null)" != "$PLATFORM" ]; then
    rm -rf "${DEPS:?}/$id"
    curl -sfL -o "$DEPS/$id.zip" "https://api.consulo.io/repository/download?id=$id&platformVersion=$PLATFORM&version=SNAPSHOT&channel=nightly&platformBuildSelect=true" || { echo "no build of $id for $PLATFORM"; exit 1; }
    unzip -q -o "$DEPS/$id.zip" -d "$DEPS" && rm "$DEPS/$id.zip"
    echo "$PLATFORM" > "$DEPS/$id.platform"
    echo "$PLATFORM" > "$DEPS/$id.version"
  fi
  local dep
  for dep in $(for jar in "$DEPS/$id"/lib/*.jar; do unzip -p "$jar" META-INF/plugin.xml 2>/dev/null; done | grep -v 'optional="true"' | sed -nE 's#.*<depends>([^<]+)</depends>.*#\1#p' | sort -u); do
    if [ ! -d "$DEPS/$dep" ] || [ "$(cat "$DEPS/$dep.platform" 2>/dev/null)" != "$PLATFORM" ]; then
      fetch_plugin "$dep"
    fi
  done
}
fetch_plugin consulo.java
fetch_plugin org.jetbrains.fernflower
fetch_plugin com.intellij.regexp
fetch_plugin com.intellij.junit
fetch_plugin com.intellij.properties
fetch_plugin org.jetbrains.idea.maven
fetch_plugin org.jetbrains.plugins.gradle

# overlay: consulo.java modules changed locally for the port and not yet published (JvmLanguage in java-language-api,
# debugger APIs in java-debugger-impl, the generate.tostring export of the plugin module consulo.java)
for artifact in consulo.java-java.language.api consulo.java-java.compiler.impl consulo.java-java.debugger.impl consulo.java; do
  local_jar=$(ls ~/.m2/repository/consulo/plugin/$artifact/3-SNAPSHOT/$artifact-3-SNAPSHOT.jar)
  cp "$local_jar" "$DEPS/consulo.java/lib/$artifact-3-SNAPSHOT.jar"
  python3 "$REPO/tools/reindex_plugin_jar.py" "$DEPS/consulo.java" "$artifact-3-SNAPSHOT.jar"
done

# overlay: consulo-gradle modules changed locally for the port (Scala compiler plugins in the tooling ScalaModel,
# GradleNotificationGroups in gradle-api); build them with `mvn install -DskipTests` in consulo-gradle (plugin with -o)
GRADLE_M2=~/.m2/repository/consulo/plugin
for module in "" -gradle.api -tooling.extension.api; do
  jar=org.jetbrains.plugins.gradle$module-3-SNAPSHOT.jar
  cp "$GRADLE_M2/org.jetbrains.plugins.gradle$module/3-SNAPSHOT/$jar" "$DEPS/org.jetbrains.plugins.gradle/lib/$jar"
  python3 "$REPO/tools/reindex_plugin_jar.py" "$DEPS/org.jetbrains.plugins.gradle" "$jar"
done
# the jars injected into the Gradle daemon by the import init script
cp "$GRADLE_M2/org.jetbrains.plugins.gradle-tooling.extension.api/3-SNAPSHOT/org.jetbrains.plugins.gradle-tooling.extension.api-3-SNAPSHOT.jar" \
  "$DEPS/org.jetbrains.plugins.gradle/gradle-rt/gradle-ext-api.jar"
cp "$GRADLE_M2/org.jetbrains.plugins.gradle-tooling-extension.impl/3-SNAPSHOT/org.jetbrains.plugins.gradle-tooling-extension.impl-3-SNAPSHOT.jar" \
  "$DEPS/org.jetbrains.plugins.gradle/gradle-rt/gradle-ext-impl.jar"

# modules online; the plugin packaging offline - its dependency workspace would ask the hub for the platform build number
# of the local platform jars (may be a local 9999 build), the dependencies are fetched above instead
(cd "$REPO" && mvn -q install -pl '!plugin' -DskipTests -Dmaven.build.cache.enabled=false)
(cd "$REPO" && mvn -o -q install -pl plugin -DskipTests -Dmaven.build.cache.enabled=false)

if ! pgrep -f "Xvfb :$DISPLAY_NUM" >/dev/null; then
  Xvfb ":$DISPLAY_NUM" -screen 0 1600x1000x24 >/dev/null 2>&1 &
  sleep 1
  DISPLAY=":$DISPLAY_NUM" openbox >/dev/null 2>&1 &
fi
cd "$MODULE" && DISPLAY=":$DISPLAY_NUM" mvn consulo:run-desktop-awt-fork
