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

build the program
```bash
make build
```

Start the server:
```
bash init_db
```

run the program
```bash
make run_Blank
```

## Timing:
Timing is done by the Timer class. A `timer` object is passed to the different classes to be timed. The times are written to a file in the `temp` folder specified by the TIME variable in the make file.

the times are written in the format:
```
<instances> ; <changes> ; <solution> ; <tag> ; <time>
```

Recording a split:
```java
timer.newSplit("label", "solutionName", <instances>, <changes>)
```

- N.B.: The first split needs to have the label "start" 
- N.B.: The last  split needs to have the label "end" 


## Structure
### APP.java
  - parse command-line arguments
  - start the correct function from the `Controller` class
### Controller.java
  - initialize necessary objects 
  - run one or multiple solutions
  - validate the graphs
### Diff.java
  - read unix diff from std:in
  - parse the diff to `update-string` and `delete-string`
### FusekiInterface.java
  - query the triplestore
### Logger.java
  - Custom log class printing
  - LOGTAG
### OttrInterface.java
  - Expand instances from string or file
### Timer.java
  - Create timestamps with meta information and save to file
###  Plotter.py
   -  reads the timer file and creates plots
### Solutions
  - Rebuild.java
    - the baseline naive solution
  - SImpleUpdate.java
  - BlankNode.java