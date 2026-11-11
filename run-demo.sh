#!/usr/bin/env sh
set -eu

classes="${TMPDIR:-/tmp}/game-report-mailer-classes"
rm -rf "$classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java -name '*.java')
java -cp "$classes" gg.academy.report.GameReportDemo "$@"
