APP_DIR = /home/magnus/Emner/Master/dev/ottr-update
TEMP_DIR = /home/magnus/Emner/Master/dev/temp

OLD_INSTANCES = old_instances.stottr
NEW_INSTANCES = new_instances.stottr
TEMPLATES = templates.stottr

all: build

build: 
	cd $(APP_DIR) && mvn package

diff: 
	cd $(APP_DIR) && diff $(TEMP_DIR)/$(OLD_INSTANCES) $(TEMP_DIR)/$(NEW_INSTANCES) | java -jar target/update.jar

