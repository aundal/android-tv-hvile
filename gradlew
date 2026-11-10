#!/bin/sh
addon_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd) || exit 1
exec java -cp "$addon_dir/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain -p "$addon_dir" "$@"
