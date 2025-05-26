FROM maven:3.8.3-openjdk-17

WORKDIR /app

COPY . .

RUN mvn install:install-file -Dfile=./lib/WPDS-3.1.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=WPDS -Dversion=3.1.1 -Dpackaging=jar
RUN mvn install:install-file -Dfile=./lib/synchronizedPDS-3.1.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=synchronizedPDS -Dversion=3.1.1 -Dpackaging=jar
RUN mvn install:install-file -Dfile=./lib/pathexpression-1.0.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=pathexpression -Dversion=1.0.1 -Dpackaging=jar
RUN mvn install:install-file -Dfile=./lib/boomerangScope-NEW-3.1.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=boomerangScope-NEW -Dversion=3.1.1 -Dpackaging=jar
RUN mvn install:install-file -Dfile=./lib/boomerangPDS-3.1.1.jar -DgroupId=de.fraunhofer.iem -DartifactId=boomerangPDS -Dversion=3.1.1 -Dpackaging=jar
RUN mvn install:install-file -Dfile=./lib/JasmineCustom-1.0-SNAPSHOT.jar -DgroupId=org.unknown -DartifactId=jasmine -Dversion=1.0.0 -Dpackaging=jar

RUN mvn clean compile assembly:single

#ENTRYPOINT ["java", "-jar", "/app/target/TaintAnalysisSoot-1.0-SNAPSHOT-jar-with-dependencies.jar"]
ENTRYPOINT ["java", "-Xmx8G", "-Xss128m", "-cp", "./target/TaintAnalysisSoot-1.0-SNAPSHOT-jar-with-dependencies.jar", "user.SpringAnalysisMain"]