# path to the ottr-update directory
APP_DIR = /home/prebz/ottr/dev/ottr-update
# path to the temp directory
TEMP_DIR = /home/prebz/ottr/dev/temp

TEMPLATES = planet.stottr
TIMER = times.txt
SOURCE = "exoplanets.stottr"
N = "10, 100, 500, 1000, 2000, 3000, 4000, 4500"
DELETION_NUMBER = 2
CHANGE_NUMBER = 0
INSERT_NUMBER = 0
TOTAL = 2


build: 
	cd $(APP_DIR) && mvn package

run: 
	cd $(APP_DIR) && java -jar target/update.jar $(SOURCE) $(N) $(TOTAL)

plot:
	python3 Plotter.py $(TEMP_DIR)/$(TIMER)

create:
	python3 StringDataCreator.py $(SOURCE) $(N) $(DELETION_NUMBER) $(CHANGE_NUMBER) $(INSERT_NUMBER)

test: build create run plot