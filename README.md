# OTTR Update algorithm

## Setup
init-db converts a set of ottr instances and templates to an RDF-file and initializes a database with the result. 

init-db requires certian variables. These are located in the **variables** script in the **temp** folder. 
Example_temp contains all required variables and files. Set the path-variables and rename the folder to temp, then you can run the different scripts.
Note that all files in the temp folder will be ignored by git. 

### Requirements:
* Lutra (https://gitlab.com/ottr/lutra/lutra)
* Apache Jena Fuseki (https://jena.apache.org/download/)
* Apache Jena (https://jena.apache.org/download/)

## How to run
Command to run inital setup:
```
TODO
```

Command to initiallize database:
```
bash init-db
```

Command to update database with SPARQL and LUTRA rebuild:
```
bash update-db
```

Command to compare two datasets. Returns equal or not equal. Dataset must be one of the following: [Original, Rebuild, Updated]
```
bash compare <dataset1> <dataset2>
```

## How to run ottr-update
Navigate into the correct directory
```bash
cd ottr-update/
```

Run the program
```bash
java -cp target/ottr-update-1.0-SNAPSHOT.jar:<PATH_TO_lutra.jar> update.ottr.App
```

## Timing:
Timing is done by the Timer class. A `timer` object is passed to the different classes to be timed. The times are written to a file in the `temp` folder specified by the TIME variable in the make file.

the times are written in the format:
```
<instances> ; <changes> ; <solution> ; <tag> ; <time>
```

- N.B.: The first split need to have the label "start" 
- N.B.: The last  split need to have the label "end" 

Recording a split:
```java
timer.newSplit("label", "solutionName", <instances>, <changes>)
```
