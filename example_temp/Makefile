# path to the ottr-update directory
APP_DIR = /home/magnus/Emner/Master/dev/ottr-update
# path to the temp directory
TEMP_DIR = /home/magnus/Emner/Master/dev/temp

OLD_INSTANCES = old_instances.stottr
NEW_INSTANCES = new_instances.stottr
TEMPLATES = templates.stottr
TIMER = times.txt
SOURCE = "exoplanets.stottr"
N = "10, 100, 1000"
DELETION_NUMBER = 2
CHANGE_NUMBER = 5
INSERT_NUMBER = 11

all: build

build: 
	cd $(APP_DIR) && mvn package

diff: 
	cd $(APP_DIR) && diff $(TEMP_DIR)/$(OLD_INSTANCES) $(TEMP_DIR)/$(NEW_INSTANCES) | java -jar target/update.jar

test:
	cd $(APP_DIR) && diff $(TEMP_DIR)/$(OLD_INSTANCES) $(TEMP_DIR)/$(NEW_INSTANCES)

plot:
	python3 Plotter.py $(TEMP_DIR)/$(TIMER)

create:
	python3 StringDataCreator.py $(SOURCE) $(N) $(DELETION_NUMBER) $(CHANGE_NUMBER) $(INSERT_NUMBER)
