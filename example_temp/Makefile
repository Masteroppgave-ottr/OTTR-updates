# general information
APP_DIR = /home/prebz/ottr/dev/ottr-update/
DB_DIR = /home/prebz/ottr/apache-jena-fuseki-4.5.0
PORT = 3030
 
# default inputs
TEMP_DIR = /home/prebz/ottr/dev/temp/
# all files are automatically annotated with either old_ or new_
INSTANCE_FILE = "exoplanets.stottr"
TEMPLATE_FILE = "template_default.stottr"
# INSTANCE_FILE = "instances.stottr"
# TEMPLATE_FILE = "templates.stottr"
TIMER = times.txt
DB_URL = http://localhost:3030/
# SOLUTIONS = "SIMPLE, REBUILD"
SOLUTIONS = "BLANK, REBUILD, SIMPLE, DUPLICATE"
WARMUP_SECONDS = 20
ALL = $(TEMP_DIR) $(INSTANCE_FILE) $(TEMPLATE_FILE) $(TIMER) $(DB_URL) $(SOLUTIONS) $(WARMUP_SECONDS)


# generate with varying number of instances
INSTANCE_NR = "2000"
DELETION_NUMBER = 1
CHANGE_NUMBER = 0
INSERT_NUMBER = 0

# generate with varying number of changes
INSTANCE_NR_2 = 10000
DELETION_NUMBERS = "50, 100"
CHANGE_NUMBERS = "0, 0"	
INSERT_NUMBERS = "50, 100"

build:
	cd $(APP_DIR) && mvn package > /dev/null

run_default:
	cd $(APP_DIR) && java -jar target/update.jar default $(ALL)

create_input_nInstances:
	python3 StringDataCreator.py n=instances $(TEMP_DIR) $(INSTANCE_FILE) $(INSTANCE_NR) $(DELETION_NUMBER) $(CHANGE_NUMBER) $(INSERT_NUMBER)

run_nInstances: 
	cd $(APP_DIR) && java -jar target/update.jar n=instances $(ALL) $(INSTANCE_NR) $(DELETION_NUMBER) $(CHANGE_NUMBER) $(INSERT_NUMBER)

plot_nInstances:
	python3 Plotter.py n=instances $(TEMP_DIR)$(TIMER)

create_input_nChanges:
	python3 StringDataCreator.py n=changes $(TEMP_DIR) $(INSTANCE_FILE)  $(INSTANCE_NR_2) $(DELETION_NUMBERS) $(CHANGE_NUMBERS) $(INSERT_NUMBERS)

run_nChanges: 
	cd $(APP_DIR) && java -jar target/update.jar n=changes $(ALL) $(INSTANCE_NR_2) $(DELETION_NUMBERS) $(CHANGE_NUMBERS) $(INSERT_NUMBERS)


plot_nChanges:
	python3 Plotter.py n=changes $(TEMP_DIR)$(TIMER)

run_Blank:
	cd $(APP_DIR) && java -jar target/update.jar blank $(ALL)	

full_test_nInstances: build create_input_nInstances run_nInstances plot_nInstances
full_test_nChanges: build create_input_nChanges run_nChanges plot_nChanges

run_plot_nInstances: run_nInstances plot_nInstances
run_plot_nChanges: run_nChanges plot_nChanges

create_run_nInstances: create_input_nInstances run_nInstances plot_nInstances
create_run_nChanges: create_input_nChanges run_nChanges plot_nChanges

init_db_gnome:
	gnome-terminal --working-directory=$(DB_DIR) -- bash init-db $(PORT) $(DB_DIR)

# this sould be done on a different terminal
init_db:
	bash init-db $(PORT) $(DB_DIR)

