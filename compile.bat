@echo off
echo Compiling Java files...

rem Create output directory
if not exist target mkdir target
if not exist target\classes mkdir target\classes

rem Download PDFBox if not exists
if not exist lib mkdir lib
if not exist lib\pdfbox-3.0.1.jar (
    echo Downloading PDFBox...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/3.0.1/pdfbox-3.0.1.jar' -OutFile 'lib\pdfbox-3.0.1.jar'"
)
if not exist lib\commons-logging-1.2.jar (
    echo Downloading commons-logging...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar' -OutFile 'lib\commons-logging-1.2.jar'"
)

rem Compile
javac -cp "lib\*" -d target\classes src\main\java\com\example\*.java

rem Create CLI JAR
echo Creating CLI JAR...
jar cfe target\jpg-to-pdf-cli.jar com.example.JpgToPdfConverter -C target\classes .
cd lib
jar uf ..\target\jpg-to-pdf-cli.jar *.jar
cd ..

rem Create GUI JAR
echo Creating GUI JAR...
jar cfe target\jpg-to-pdf-gui.jar com.example.ImageToPdfGui -C target\classes .
cd lib
jar uf ..\target\jpg-to-pdf-gui.jar *.jar
cd ..

echo Done! JARs created in target/ folder.
