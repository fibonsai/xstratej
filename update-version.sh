#!env bash
mvn -N versions:set -DnewVersion=$(<VERSION)
git tag $(<VERSION) -m "Release $(<VERSION)"
git push origin $(<VERSION)
