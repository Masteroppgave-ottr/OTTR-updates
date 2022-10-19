APP_DIR = /home/magnus/Emner/Master/dev/ottr-update # path to the ottr-update directory
TEMP_DIR = /home/magnus/Emner/Master/dev/temp		# path to the temp directory

OLD_INSTANCES = old_instances.stottr
NEW_INSTANCES = new_instances.stottr
TEMPLATES = templates.stottr

all: build

build: 
	cd $(APP_DIR) && mvn package

diff: 
	cd $(APP_DIR) && diff $(TEMP_DIR)/$(OLD_INSTANCES) $(TEMP_DIR)/$(NEW_INSTANCES) | java -jar target/update.jar


