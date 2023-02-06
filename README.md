# OTTR Update algorithm

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

## Configure the makefile
In the example_temp directory, we see an example of a makefile.

The default inputs have to be specified:
- `TEMPDIR`: the absolute path to your temp directory
- `INSTANCE_FILE`: the filename of the instance file. 
  - The filename is automatically appended "new_" or "old_"
- `TEMPLATE_FILE`: the filename of the file containing templates
- `TIMER`: the name of the file where timestamps are saved
- `SOLUTIONS`: String of space-separated solution names.

NB: INSTANCE_FILE, TEMPLATE_FILE and TIMER have to be stored in the temp directory. 


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
