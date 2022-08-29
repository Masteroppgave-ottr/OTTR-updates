# The PreMa algorithm

## Setup
init-db converts a set of ottr instances and templates to an RDF-file and initializes a database with the result. 

init-db requires certian variables. It is recommended to make a script "variables" in a folder called temp, as that is where the init-db tries to find variables. 
Note that all files in the temp folder will be ignored by git. 

### Required variables:
* **path_to_db**    -path to folder with fuseki database
* **path_to_input** -path to folder with inputfiles 
* **instances**     -filename of instances
* **templates**     -filename of templates
* **output**        -filename of output

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

Command to compare Rebuild and Update datasets to see if they are equal.
```
TODO
```
