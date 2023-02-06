# general information
APP_DIR = /home/magnus/Emner/Master/dev/ottr-update/
DB_DIR = /home/magnus/Emner/Master/apache-jena-fuseki-4.5.0
PORT = 3030

# default inputs
TEMP_DIR = /home/magnus/Emner/Master/dev/temp/
# all files are automatically annotated with either old_ or new_
INSTANCE_FILE = "initme.stottr"
TEMPLATE_FILE = "templateme.stottr"
TIMER = times.txt
DB_URL = http://localhost:3030/
# SOLUTIONS = "SIMPLE, REBUILD"
SOLUTIONS = "SIMPLE"
ALL = $(TEMP_DIR) $(INSTANCE_FILE) $(TEMPLATE_FILE) $(TIMER) $(DB_URL) $(SOLUTIONS)


# generator parameters with varying number of instances
INSTANCE_NR = "10, 20, 30"
DELETION_NUMBER = 2
CHANGE_NUMBER = 0
INSERT_NUMBER = 0
TOTAL_CHANGES = 2

# generator parameters with varying number of changes
INSTANCE_NR_2 = 10000
DELETION_NUMBERS = "50, 100"
CHANGE_NUMBERS = "0, 0"
INSERT_NUMBERS = "50, 100"

build:
	cd $(APP_DIR) && mvn package

create_nInstances:
	python3 StringDataCreator.py n=instances $(INSTANCE_FILE) $(INSTANCE_NR) $(DELETION_NUMBER) $(CHANGE_NUMBER) $(INSERT_NUMBER)

run_nInstances: 
	cd $(APP_DIR) && java -jar target/update.jar n=instances $(ALL) $(INSTANCE_NR) $(TOTAL_CHANGES)

plot_nInstances:
	python3 Plotter.py n=instances $(TEMP_DIR)$(TIMER)

create_nChanges:
	python3 StringDataCreator.py n=changes $(INSTANCE_FILE) $(INSTANCE_NR_2) $(DELETION_NUMBERS) $(CHANGE_NUMBERS) $(INSERT_NUMBERS)

run_nChanges: 
	cd $(APP_DIR) && java -jar target/update.jar n=changes $(ALL) $(INSTANCE_NR_2) $(DELETION_NUMBERS) $(CHANGE_NUMBERS) $(INSERT_NUMBERS)

# name | old instances | new instances | templates 
run_Blank:
	cd $(APP_DIR) && java -jar target/update.jar blank $(ALL)

plot_nChanges:
	python3 Plotter.py n=changes $(TEMP_DIR)$(TIMER)

test_nInstances: build create_nInstances run_nInstances plot_nInstances
test_nChanges: build create_nChanges run_nChanges plot_nChanges

retest_nInstances: run_nInstances plot_nInstances
retest_nChanges: run_nChanges plot_nChanges

rerun_nInstances: create_nInstances run_nInstances plot_nInstances
rerun_nChanges: create_nChanges run_nChanges plot_nChanges

init_db:
	gnome-terminal --working-directory=$(DB_DIR) -- ./fuseki-server --port $(PORT) --update
