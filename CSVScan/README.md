# CSVScan

## Manual Installation

- Install libraries under 'CSVScan/lib'
   - `mvn install:install-file -Dfile=./WPDS-3.1.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=WPDS -Dversion=3.1.1 -Dpackaging=jar`
   - `mvn install:install-file -Dfile=./synchronizedPDS-3.1.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=synchronizedPDS -Dversion=3.1.1 -Dpackaging=jar`
   - `mvn install:install-file -Dfile=./pathexpression-1.0.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=pathexpression -Dversion=1.0.1 -Dpackaging=jar`
   - `mvn install:install-file -Dfile=./boomerangScope-NEW-3.1.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=boomerangScope -Dversion=3.1.1 -Dpackaging=jar`
   - `mvn install:install-file -Dfile=./boomerangPDS-3.1.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=boomerangPDS -Dversion=3.1.1 -Dpackaging=jar`
   - `mvn install:install-file -Dfile=./JasmineCustom-1.0-SNAPSHOT.jar -DgroupId=org.unknown -DartifactId=jasmine -Dversion=1.0.0 -Dpackaging=jar`
- Install the Maven project with the pom.xml
    - Do not change `Google Guava 33.0.0` and `Soot 4.5.0`

