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

Command to compare Rebuild and Update datasets to see if they are equal.
```
bash compare
```
