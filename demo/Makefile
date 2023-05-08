# paths
APP_DIR = ../ottr-update/
# DB_DIR = #TODO! add path to the fuseki server (e.g. /home/magnus/Emner/Master/apache-jena-fuseki-4.5.0/)
PORT = 3030

# standard variables
TEMP_DIR = "./"
GENERATED_DIR = "./generated/"
INSTANCE_FILE = "exoplanets.stottr"
TIMER = times.txt
DB_URL = http://localhost:${PORT}/

run: build test_varying_normal_instances plot_nInstances

build:
	cd $(APP_DIR) && mvn package

init_db:
	../init-db $(PORT) $(DB_DIR)

test_varying_normal_instances:
	java -jar $(APP_DIR)target/update.jar n=instances $(TEMP_DIR) ./varying_normal_instances/ $(INSTANCE_FILE) template_default.stottr $(TIMER) $(DB_URL) "SIMPLE, REBUILD" 0 "250, 500, 1000, 2500, 5000" 10 0 10

test_varying_normal_changes:
	java -jar $(APP_DIR)target/update.jar n=changes $(TEMP_DIR) ./varying_normal_changes/ $(INSTANCE_FILE) template_default.stottr $(TIMER) $(DB_URL) "SIMPLE, REBUILD" 0 "5000" "25, 50, 100, 250, 500" "0, 0, 0, 0, 0" "25, 50, 100, 250, 500"

test_varying_duplicate_changes:
	java -jar $(APP_DIR)target/update.jar n=changes $(TEMP_DIR) ./varying_duplicate_changes/ $(INSTANCE_FILE) template_duplicates.stottr $(TIMER) $(DB_URL) "DUPLICATE, REBUILD" 0 "5000" "25, 50, 100, 250, 500" "0, 0, 0, 0, 0" "25, 50, 100, 250, 500"

test_varying_blank_changes:
	java -jar $(APP_DIR)target/update.jar n=changes $(TEMP_DIR) ./varying_blank_changes/ $(INSTANCE_FILE) template_default.stottr $(TIMER) $(DB_URL) "BLANK, REBUILD" 0 "5000" "5, 10, 15, 20, 25" "0, 0, 0, 0, 0" "5, 10, 15, 20, 25"

plot_nInstances:
	python3 ../Plotter.py n=instances $(TIMER) "Number of instances"

plot_nChanges:
	python3 ../Plotter.py n=changes $(TIMER) "Number of changes"