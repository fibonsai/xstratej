#!env bash
mvn -N versions:set -DnewVersion=$(<VERSION)
mvn versions:commit
git ch -b release-$(<VERSION)
git commit -m "Release $(<VERSION)" VERSION `find . -name pom.xml | xargs echo -n`
git tag $(<VERSION) -m "Release $(<VERSION)"
git push origin release-$(<VERSION) 
git push origin $(<VERSION)
