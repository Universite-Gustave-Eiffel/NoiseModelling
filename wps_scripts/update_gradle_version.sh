#!/bin/bash

# Set the path to the pom.xml file
POM_FILE="../pom.xml"

# Set the path to the build.gradle file
GRADLE_FILE="build.gradle"

# Check if the pom.xml file exists
if [ ! -f "$POM_FILE" ]; then
  echo "Error: pom.xml file not found in the root directory."
  exit 1
fi

# Check if the build.gradle file exists
if [ ! -f "$GRADLE_FILE" ]; then
  echo "Error: build.gradle file not found at $GRADLE_FILE"
  exit 1
fi

# Extract the version from the pom.xml file
VERSION=$(xpath -q -e '/project/version/text()' "$POM_FILE" 2>/dev/null)

# Check if xpath is installed
if [ $? -ne 0 ]; then
    echo "Error: xpath is not installed. Please install it using 'apt-get install -y xmlstarlet' or equivalent."
    exit 1
fi

# Check if a version was found
if [ -z "$VERSION" ]; then
  echo "Error: Could not extract version from pom.xml"
  exit 1
fi

# Remove the "-SNAPSHOT" suffix if it exists
NM_VERSION="${VERSION%-SNAPSHOT}"

# Construct the replacement string
REPLACEMENT="def nm_version='${NM_VERSION}'"

# Use sed to replace the line in the build.gradle file
awk -v new_version="$NM_VERSION" '
  /def nm_version=/ {
    print "def nm_version='"'"'" new_version "'"'"'"
    next
  }
  { print }
' "$GRADLE_FILE" > "${GRADLE_FILE}.tmp" && mv "${GRADLE_FILE}.tmp" "$GRADLE_FILE"

echo "Successfully updated nm_version in $GRADLE_FILE to $NM_VERSION"
