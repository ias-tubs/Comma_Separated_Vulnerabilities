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

## Structure CSVScan

- 'aliasing': Classes to handle aliasing
- 'analysis': Core functionality to analyse data flows
- 'user': Main class to start the analysis
- 'util': Helper functions

## Usage Instructions

To analyze a spring-based web application follow these steps:
1. Build the taint analysis with `mvn clean compile assembly:single` to get a executable jar. The executable is stored under 'target'.
2. Build the war/jar file of the application you want to analyze
3. Unpack the file so that you should have a 'WEB-INF' or 'BOOT-INF' folder with a folder for classes and a folder for libraries
4. If this is not the first analysis, remove the intermediate results of the previous run ('classes_with_tainted_repo.json' and 'sources.json')
5. Depending on the analysis, the sinks in 'sinks.json' must be adapted
5. Start the first run: `java -cp ./target/TaintAnalysisSoot-1.0-SNAPSHOT-jar-with-dependencies.jar user.SpringAnalysisMain [APP_NAME] [PATH_TO_CLASSES] [PATH_TO_LIBS]`, APP_NAME can be freely selected
6. Start the second run: `java -cp ./target/TaintAnalysisSoot-1.0-SNAPSHOT-jar-with-dependencies.jar user.SpringAnalysisMain [APP_NAME] [PATH_TO_CLASSES] [PATH_TO_LIBS] | tee [APP_NAME].txt`
7. The results are written to the text file [APP_NAME].txt
