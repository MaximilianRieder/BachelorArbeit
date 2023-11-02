-Name and goal of the project

Build: (in IDE or with command line in directory next to POM.xml -> /rieder)
maven clean
maven package
-> .jar
needed files(put them next to the jar)(all found in \rieder\src\main\resources):
	-calendar_dates.csv
	-routes.csv
	-trips.csv
	-stops.csv
	-stoptimes.csv
	-SEL_FZT_FELD.csv
	-REC_ORT.csv
	-REC_FRT.csv
	-configDayDate.yaml
	-fileExceptionsStandardData.yaml
	-fileExceptionsTestData.yaml
	
execute jar-with-dependencys

How to execute the code:

-h / -help for help
-c / -conditions for checking the fifo conditions and if there are overlaps in location names -> write them in exception yaml
-p / -print to print the necessary algorithm steps and the path on the console
-s / -start, -t / -target, -d / -daytime are used for local execution
    example format: -s Dachauplatz -t Universitdt -d 07:30:00
-r / -rest to start a rest server, which can be used with this call (no other arguments considered except -help)
    server:port/api/shortestway/{identifyerFrom}/{identifyerTo}/{time}
    (example): http://localhost:8080/api/shortestway/1010/1020/00:00:00

information about the location names and the associated identifier are found in nameIdentStandardData.txt and nameIdentTestData.txt
-createNameIdentFiles create the name identifier files (no other arguments considered and only local execution)

-updateRoutingData to force using and update the current Dataset (has to be done if you change the day)

configDayDate.yaml configures which dataset is used and which day (ARGUMENTS LIKE -createNameIdentFiles ONLY APPLIED ON SPECIFIED DATA SET)
    if testdata: true -> write day of the year 04.05.2020 -> 20200504
    if testdata: false -> write weekday monday-sunday -> 1-7

fileExceptions.yaml / fileExceptionsTestData.yaml (standardData/testData) allows to...
    ...removeStoppingPointsIdentifiers -> remove stopping points with these identifiers
    ...transferExceptions -> change the transfer times of stopping points with these identifiers
    ...locationNameOverlaps -> rename stopping points with these identifiers
!!!information about the location names and the associated identifier are found in nameIdentStandardData and nameIdentTestData!!!