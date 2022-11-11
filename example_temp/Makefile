# path to the ottr-update directory
APP_DIR = /home/prebz/ottr/dev/ottr-update/
# path to the temp directory
TEMP_DIR = /home/prebz/ottr/dev/temp/

INSTANCES = "exoplanets.stottr"
TEMPLATES = "planet.stottr"
TIMER = times.txt
DB_URL = http://localhost:3030/
ALL = $(TEMP_DIR) $(INSTANCES) $(TEMPLATES) $(TIMER) $(DB_URL)


# generate with varying number of instances
N = "10, 100, 1000, 10000, 50000, 100000"
DELETION_NUMBER = 2
CHANGE_NUMBER = 0
INSERT_NUMBER = 0
TOTAL_CHANGES = 2

# generate with varying number of changes
N2 = 10000
DELETION_NUMBERS = "1, 50, 500, 1000"
CHANGE_NUMBERS = "0, 0, 0, 0"
INSERT_NUMBERS = "1, 50, 500, 1000"


build:
	cd $(APP_DIR) && mvn package

plot:
	python3 Plotter.py $(TEMP_DIR)/$(TIMER)

run_nInstances: 
	cd $(APP_DIR) && java -jar target/update.jar n=instances $(ALL) $(N) $(TOTAL_CHANGES)

create_nInstances:
	python3 StringDataCreator.py n=instances $(INSTANCES) $(N) $(DELETION_NUMBER) $(CHANGE_NUMBER) $(INSERT_NUMBER)

run_nChanges: 
	cd $(APP_DIR) && java -jar target/update.jar n=changes $(ALL) $(N2) $(DELETION_NUMBERS) $(CHANGE_NUMBERS) $(INSERT_NUMBERS)

create_nChanges:
	python3 StringDataCreator.py n=changes $(INSTANCES) $(N2) $(DELETION_NUMBERS) $(CHANGE_NUMBERS) $(INSERT_NUMBERS)

test_nInstances: build create_nInstances run_nInstances plot
test_nChanges: build create_nChanges run_nChanges plot

retest_nInstances: run_nInstances plot
retest_nChanges: run_nChanges plot
