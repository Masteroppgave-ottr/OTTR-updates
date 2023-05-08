# Efficient update of OTTR-constructed triplestores

### Requirements:
* Lutra (https://gitlab.com/ottr/lutra/lutra)
* Apache Jena Fuseki (https://jena.apache.org/download/)
* Apache Jena (https://jena.apache.org/download/)

## How to run the demo
Install Apache Jena from (https://jena.apache.org/download/)

Install Apache Jena Fuseki from (https://jena.apache.org/download/)

SET the `DB_DIR` variable in the makefile in the `/demo` directory.

Navigate to the demo folder
```bash
cd demo
```

Build the program
```bash
make build
```

In a new terminal window start the Fuseki database:
```
make init_db
```

Run one of the tests
1. run a test with a varying number of *normal instances* and a constant number of changes to *normal instances*
    - ```bash
      make test_varying_normal_instances
      ```
2. run a test with a varying number of changes to *normal instances* and a constant number of *normal instances*
    - ```bash
      make test_varying_normal_changes
      ```
3. run a test with a varying number of changes to *duplicate instances* and a constant number of *normal instances*
    - ```bash
      make test_varying_duplicate_changes
      ```
4. run a test with a varying number of changes to *blank instances* and a constant number of *normal instances*
    - ```bash
      make test_varying_blank_changes
      ```

Plot the result of point 1
```bash
make plot_nInstances
```

Plot the results of points 2, 3 or 4
```bash
make plot_nChanges
```

<!-- ## Configure the makefile
In the example_temp directory, we see an example of a makefile.

The default inputs have to be specified:
- `TEMPDIR`: the absolute path to your temp directory
- `INSTANCE_FILE`: the filename of the instance file. 
  - The filename is automatically appended "new_" or "old_"
- `TEMPLATE_FILE`: the filename of the file containing templates
- `TIMER`: the name of the file where timestamps are saved
- `SOLUTIONS`: String of space-separated solution names.

NB: INSTANCE_FILE, TEMPLATE_FILE and TIMER have to be stored in the temp directory.  -->


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
  - Reads the timer file and creates plots
### StringDataCreator.py
  - Creates test data from the `exoplantes.stottr` seed file    
### Solutions
  - Rebuild.java
    - the baseline naive solution
  - SimpleUpdate.java
  - BlankNode.java
  - Duplicates.java

<!-- 
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
- N.B.: The last  split needs to have the label "end"  -->

## General outline of the program
<img src="https://user-images.githubusercontent.com/42439472/236768091-ebb96bbd-676f-4593-bb3d-5ea2d23ae8ad.svg" width="60%" />

## Simple solution
<img src="https://user-images.githubusercontent.com/42439472/236768266-f1d4c490-95a0-42d8-b9db-8bb919588152.svg" width="60%" />

## Duplicate solution
<img src="https://user-images.githubusercontent.com/42439472/236768335-e5b7c5d2-c05b-4b33-aae4-e89241c905b8.svg" width="60%" />

## Blank node solution
<img src="https://user-images.githubusercontent.com/42439472/236768406-eb8bbbb5-ab45-4af7-b9e1-c7cfe2cda519.svg" width="60%" />

## Combined solution
<img src="https://user-images.githubusercontent.com/42439472/236768458-0348c4c3-fdc9-4d05-987a-06db3f60dd15.svg" width="60%" />

