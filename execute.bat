echo "Installing pedviz_0.15.jar..."

call mvn install:install-file -Dfile=lib/pedviz_0.15.jar -DgroupId=org.sid.ontview -DartifactId=pedviz -Dversion=0.15 -Dpackaging=jar

echo "Installing KCE-0.0.1-SNAPSHOT.jar..."

call mvn install:install-file -Dfile=lib/kce/KCE-0.0.1-SNAPSHOT.jar -DgroupId=org.sid.ontview -DartifactId=kce -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar

mvn clean compile exec:java -Dexec.args="-Dprism.targetvram=2G -Xmx8G -Dprism.order=es2 -Dprism.forceGPU --add-opens javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED"