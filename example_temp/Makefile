# General information
APP_DIR = /home/magnus/Emner/Master/dev/ottr-update/
DB_DIR = /home/magnus/Emner/Master/apache-jena-fuseki-4.5.0/
PORT = 3030

# Default inputs
TEMP_DIR = /home/magnus/Emner/Master/dev/temp/
INSTANCE_FILE = "exoplanets.stottr"    # all files are automatically annotated with either old_ or new_
TEMPLATE_FILE = "template_default.stottr"
TIMER = times.txt
DB_URL = http://localhost:3030/
SOLUTIONS = "BLANK"
WARMUP_SECONDS = 0
ALL = $(TEMP_DIR) $(INSTANCE_FILE) $(TEMPLATE_FILE) $(TIMER) $(DB_URL) $(SOLUTIONS) $(WARMUP_SECONDS)

# Generator parameters with varying number of instances
INSTANCE_NR = "250, 500, 1000, 2500, 5000, 10000, 15000, 20000, 30000, 40000, 50000"
DELETION_NUMBER = 0
CHANGE_NUMBER = 0
INSERT_NUMBER = 0
DUPLICATE_PAIR_INSERT_NUMBER = 0
DUPLICATE_PAIR_DELETE_NUMBER = 0
BLANK_INSERT_NUMBER = 0
BLANK_DELETE_NUMBER = 5


# Generator parameters with varying number of changes
INSTANCE_NR_2 = 20000
DELETION_NUMBERS = "250, 500, 1000, 2500, 5000, 7500, 10000"
CHANGE_NUMBERS = "0, 0, 0, 0, 0, 0, 0"
INSERT_NUMBERS = "250, 500, 1000, 2500, 5000, 7500, 10000"

# Generator parameters with varying number of duplicates
INSTANCE_NR_3 = 20000
DUP_DELETION_NUMBERS = "25"#"25, 50, 100, 150, 200, 250, 300, 350"
DUP_CHANGE_NUMBERS = "0"#"0, 0, 0, 0, 0, 0, 0, 0"
DUP_INSERT_NUMBERS = "25"#"25, 50, 100, 150, 200, 250, 300, 350"

# Generator parameters with varying number of blank nodes
INSTANCE_NR_4 = 20000
BLANK_DELETION_NUMBERS = "20"#"10, 20, 30, 40, 50, 60, 70, 80"
BLANK_CHANGE_NUMBERS = "0"#"0, 0, 0, 0, 0, 0, 0, 0"
BLANK_INSERT_NUMBERS = "20"#"0, 0, 0, 0, 0, 0, 0, 0"

# The x labels used in the plots
X_LABEL_N_CHANGES = "Number of blank insertions/deletions"
X_LABEL_N_INSTANCES = "Number of instances"


# Default rules
build:
	cd $(APP_DIR) && mvn package > /dev/null

init_db:
	gnome-terminal -- ./init-db $(PORT) $(DB_DIR)

run_default:
	cd $(APP_DIR) && java -jar target/update.jar default $(ALL)


# Create input
create_input_nInstances:
	python3 StringDataCreator.py n=instances $(TEMP_DIR) $(INSTANCE_FILE) $(INSTANCE_NR) $(DELETION_NUMBER) $(CHANGE_NUMBER) $(INSERT_NUMBER) $(DUPLICATE_PAIR_INSERT_NUMBER) $(DUPLICATE_PAIR_DELETE_NUMBER) $(BLANK_INSERT_NUMBER) $(BLANK_DELETE_NUMBER)

create_input_nChanges:
	python3 StringDataCreator.py n=changes $(TEMP_DIR) $(INSTANCE_FILE)  $(INSTANCE_NR_2) $(DELETION_NUMBERS) $(CHANGE_NUMBERS) $(INSERT_NUMBERS)

create_input_nDuplicates:
	python3 StringDataCreator.py n=duplicates $(TEMP_DIR) $(INSTANCE_FILE)  $(INSTANCE_NR_3) $(DUP_DELETION_NUMBERS) $(DUP_INSERT_NUMBERS) 

create_input_nBlanks:
	python3 StringDataCreator.py n=blanks $(TEMP_DIR) $(INSTANCE_FILE)  $(INSTANCE_NR_4) $(BLANK_DELETION_NUMBERS) $(BLANK_INSERT_NUMBERS) 


# Run the program
run_nInstances: 
	cd $(APP_DIR) && java -jar target/update.jar n=instances $(ALL) $(INSTANCE_NR) $(DELETION_NUMBER) $(CHANGE_NUMBER) $(INSERT_NUMBER)

run_nChanges: 
	cd $(APP_DIR) && java -jar target/update.jar n=changes $(ALL) $(INSTANCE_NR_2) $(DELETION_NUMBERS) $(CHANGE_NUMBERS) $(INSERT_NUMBERS)

run_nDuplicates:
		cd $(APP_DIR) && java -jar target/update.jar n=changes $(ALL) $(INSTANCE_NR_3) $(DUP_DELETION_NUMBERS) $(DUP_CHANGE_NUMBERS) $(DUP_INSERT_NUMBERS)

run_nBlanks:
		cd $(APP_DIR) && java -jar target/update.jar n=changes $(ALL) $(INSTANCE_NR_4) $(BLANK_DELETION_NUMBERS) $(BLANK_CHANGE_NUMBERS) $(BLANK_INSERT_NUMBERS)


# Plot rules
plot_nInstances:
	python3 Plotter.py n=instances $(TEMP_DIR)$(TIMER) $(X_LABEL_N_INSTANCES)
plot_nChanges:
	python3 Plotter.py n=changes $(TEMP_DIR)$(TIMER) $(X_LABEL_N_CHANGES)


# Combined rules
full_test_nInstances: build create_input_nInstances run_nInstances plot_nInstances
full_test_nChanges: build create_input_nChanges run_nChanges plot_nChanges
full_test_nDuplicates: build create_input_nDuplicates run_nDuplicates plot_nChanges
full_test_nBlanks: build create_input_nBlanks run_nBlanks plot_nChanges

create_run_nInstances: create_input_nInstances run_nInstances plot_nInstances
create_run_nChanges: create_input_nChanges run_nChanges plot_nChanges
create_run_nDuplicates: create_input_nDuplicates run_nDuplicates plot_nChanges
create_run_nBlanks: create_input_nBlanks run_nBlanks plot_nChanges

run_plot_nInstances: run_nInstances plot_nInstances
run_plot_nChanges: run_nChanges plot_nChanges
run_nDuplicates: run_nDuplicates plot_nChanges
run_nBlanks: run_nBlanks plot_nChanges
