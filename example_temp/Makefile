APP_DIR = /home/magnus/Emner/Master/dev/ottr-update/

# default inputs
TEMP_DIR = /home/magnus/Emner/Master/dev/temp/
INSTANCES = "exoplanets.stottr"
TEMPLATES = "planet.stottr"
TIMER = times.txt
DB_URL = http://localhost:3030/
ALL = $(TEMP_DIR) $(INSTANCES) $(TEMPLATES) $(TIMER) $(DB_URL)


# generate with varying number of instances
N = "10, 100, 1000"
DELETION_NUMBER = 2
CHANGE_NUMBER = 0
INSERT_NUMBER = 0
TOTAL_CHANGES = 2

# generate with varying number of changes
N2 = 10
DELETION_NUMBERS = "1, 2, 3, 4, 5"
CHANGE_NUMBERS = "1, 2, 3, 4, 5"
INSERT_NUMBERS = "0, 0, 0, 0, 0"


build:
	cd $(APP_DIR) && mvn package

plot:
	python3 Plotter.py $(TEMP_DIR)/$(TIMER)

run_nInstances: 
	cd $(APP_DIR) && java -jar target/update.jar "n=instances" $(ALL) $(N) $(TOTAL_CHANGES)

create_nInstances:
	python3 StringDataCreator.py "n=instances" $(INSTANCES) $(N) $(DELETION_NUMBER) $(CHANGE_NUMBER) $(INSERT_NUMBER)

run_nChanges: 
	cd $(APP_DIR) && java -jar target/update.jar "n=changes" $(ALL) $(N2) 1

create_nChanges:
	python3 StringDataCreator.py "n=changes" $(INSTANCES) $(N2) $(DELETION_NUMBERS) $(CHANGE_NUMBERS) $(INSERT_NUMBERS)

test_nInstances: build create_nInstances run_nInstances plot
test_nChanges: build create_nChanges run_nChanges plot

retest_nInstances: run_nInstances plot
retest_nChanges: run_nChanges plot