# OTTR Update algorithm

## Setup
init-db converts a set of ottr instances and templates to an RDF-file and initializes a database with the result. 

init-db requires certian variables. These are located in the **variables** script in the **temp** folder. 
Note that all files in the temp folder will be ignored by git. 

### Required variables:
* **path_to_db**    -path to folder with fuseki database
* **temp_files**    -folder with temporary files, such as input- and outputfiles
* **path_to_bin**     -path to bin folder in fuseki 
* **path_to_jena_bin** -path to bin folder in jena without fuseki
* **lutra**         -path to lutra.jar

* **instances**     -filename of instances
* **templates**     -filename of templates
* **output**        -filename of output from init
* **rebuildOutput** -filename of output from rebuild
* **query**         -filename of custom SPARQL query
* **updateQuery**   -filename of SPARQL update query

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
bash compare
```
