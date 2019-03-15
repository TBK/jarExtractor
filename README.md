# jarExtractor
Tool to extract .class files from a .jar that has long filenames from obfuscation.

To be used in cojunction with tools like zipinfo and [CFR](https://www.benf.org/other/cfr/).

## Use
```
Proper use:
java jarExtractor <jarToExtract.jar>
java jarExtractor <jarToExtract.jar> <Cap>
java jarExtractor <jarToExtract.jar> <Cap> <Pattern> <PatternReplacement>
--------------------------------------------
<jarToExtract.jar>: relative or absolute file path.
By default the name will be capped at 80 chars and use deflate to create a new name.
-----------
The options below are an manual overwrite to the default cap/rename.
<Cap>: The cut of length for entries before using the pattern and replacement.
<Pattern>: The pattern will be cut out of mathing files and dirs names.
<PatternReplacementr>: Regex to generate a new shorter name.
--------------------------------------------
Get the pattern with: zipinfo jarToExtract.jar > files.txt
A pattern extracted from files.txt could be ^0OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO
It is also possible to use more complex regex - e.g. ^.*O{100}.*
```

### Example
This example is a [Alpine Linux](https://alpinelinux.org/) box:
```
sudo apk add openjdk8 unzip
mkdir work && cd work
curl -LJO https://www.benf.org/other/cfr/cfr-0.140.jar
java jarExtractor jarToExtract.jar
find . -type f -name "*.class" -exec java -jar cfr-0.140.jar {} --outputdir {}.java \;
```

## Build
```
javac jarExtractor.java
```
