#!/bin/sh
set -eu

classes="${TMPDIR:-/tmp}/legal-key-rotation-classes"
mkdir -p "$classes"
find src/main/java src/test/java -name '*.java' -print | xargs javac -d "$classes"

if [ "${1:-test}" = "live" ]; then
  : "${TEMPORARY_KEY_ID:?Set TEMPORARY_KEY_ID to an existing disposable key id (never the authorizing key)}"
  : "${INCIDENT_ID:?Set INCIDENT_ID}"
  java -cp "$classes" legaltech.IncidentApplication "$TEMPORARY_KEY_ID" "$INCIDENT_ID" "${GRACE_HOURS:-24}"
else
  java -cp "$classes" legaltech.matter.DeadlineFollowUpServiceTest
fi
