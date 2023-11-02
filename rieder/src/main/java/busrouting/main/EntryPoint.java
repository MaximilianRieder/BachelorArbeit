package busrouting.main;

import busrouting.configLoading.ConfigDayData;
import busrouting.configLoading.ConfigDayDataLoader;
import busrouting.main.data.*;
import busrouting.main.graph.*;
import busrouting.rest.JerseyConfiguration;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public class EntryPoint {

    private static final String JERSEY_SERVLET_NAME = "jersey-container-servlet";

    public static void main(String[] args) throws Exception {
        final OptionParser parser = new OptionParser();
        final String[] startOptions = {
                "s",
                "start"
        };
        parser.acceptsAll(Arrays.asList(startOptions), "name of the starting position").withRequiredArg();
        final String[] targetOptions = {
                "t",
                "target"
        };
        parser.acceptsAll(Arrays.asList(targetOptions), "name of the target position").withRequiredArg();
        final String[] daytimeOptions = {
                "d",
                "daytime"
        };
        parser.acceptsAll(Arrays.asList(daytimeOptions), "daytime in format 00:00:00").withRequiredArg();
        final String[] fifoOptions = {
                "c",
                "conditions"
        };
        parser.acceptsAll(Arrays.asList(fifoOptions), "the FIFO condition will be checked for the algorithm and if there are any duplicate Location Names");
        final String[] displayOptions = {
                "p",
                "print"
        };
        parser.acceptsAll(Arrays.asList(displayOptions), "algorithm steps will be printed");
        final String[] startServiceOptions = {
                "r",
                "restapi"
        };
        parser.acceptsAll(Arrays.asList(startServiceOptions), "Set up as Server, Rest Api provided");
        final String[] createNameIdentFileOptions = {
                "createNameIdentFiles"
        };
        parser.acceptsAll(Arrays.asList(createNameIdentFileOptions), "create files with names and identifiers");
        final String[] updateRoutingDataOptions = {
                "updateRoutingData"
        };
        parser.acceptsAll(Arrays.asList(updateRoutingDataOptions), "update RoutingData");
        final String[] helpOptions = {
                "h",
                "help"
        };
        parser.acceptsAll(Arrays.asList(helpOptions), "help").forHelp();

        final OptionSet options = parser.parse(args);

        //just print the help info
        if(options.has("help")) {
            printHelp();
            return;
        }

        if(options.has("restapi")) {
            if(options.has("s") || options.has("t") || options.has("d") || options.has("c") || options.has("p") || options.has("h"))
                System.out.println("!! if rest option activated other options are no longer valid !!");
            new EntryPoint().start();
        } else {
            localExec(options);
        }
    }

    private static void localExec(OptionSet options) throws IOException {
        RoutingDataProcessed routingDataProcessed;
        ConfigDayDataLoader configDayDataLoader = new ConfigDayDataLoader("configDayData.yaml");
        ConfigDayData configDayData = configDayDataLoader.getConfigDayData();
        DataInOut dataInOut = new DataInOut(configDayData.isTestData());
        routingDataProcessed = dataInOut.getRoutingDataProcessed(configDayData.getDay());

        //force update/generate RoutingDataProcessed File if option is chosen
        if (options.has("updateRoutingData")) {
            routingDataProcessed = dataInOut.makeRoutingDataPreprocessedFromData(configDayData.getDay());
        }

        //write the name ident files if option is chosen
        if (options.has("createNameIdentFiles")) {
            if (configDayData.isTestData()) {
                dataInOut.writeNameIdentFile(routingDataProcessed.getLocationInfo(), "nameIdentTestData.txt");
            } else {
                dataInOut.writeNameIdentFile(routingDataProcessed.getLocationInfo(), "nameIdentStandardData.txt");
            }
        }

        //check conditions for the algorithm if option is chosen
        ConditionChecker conditionChecker = new ConditionChecker();
        if (options.has("conditions")) {
            conditionChecker.checkUniqueLocationNames(routingDataProcessed.getLocationInfo());
            conditionChecker.checkFIFO(routingDataProcessed.getRideTimetable(), routingDataProcessed.getLocationInfo());
            conditionChecker.checkFIFO(routingDataProcessed.getRideTimetableNextDay(), routingDataProcessed.getLocationInfo());
        }

        //no parameters for execution -> just check the conditions if selected
        if (!(options.has("start") && options.has("target") && options.has("daytime"))) {
            System.out.println("use -s (start) -t (target) -d (daytime) for solving the EAP");
            return;
        }

        //setup the graph and solve the eap
        GraphSetup graphSetup = new GraphSetup(routingDataProcessed);
        if (options.has("print")) {
            graphSetup.setDisplayOnConsole(true);
        }
        Graph graph = graphSetup.setup();
        DataTransformer dataTransformer = new DataTransformer(routingDataProcessed.getLocationInfo());
        int fromIdent;
        int toIdent;
        int time;
        try {
            fromIdent = dataTransformer.getCSPIdentifierByName(options.valueOf("start").toString());
            toIdent = dataTransformer.getCSPIdentifierByName(options.valueOf("target").toString());
            time = dataTransformer.makeSecondsFromTime(options.valueOf("daytime").toString());
        } catch (Exception e) {
            System.out.println("wrong start, target or daytime");
            return;
        }

        Way way = graph.shortestWay(fromIdent, toIdent, time);
        LinkedList<WayPoint> wayPoints = way.getShortestWayPointsInOrder();
        String prevLinenr = null;
        for (WayPoint wp : wayPoints) {
            String stoppingPointName = dataTransformer.getNameByIdentifier(wp.getNode().getIdentifier());
            int timeWayPoint = wp.getTime();
            int timeWayPointDisplay = timeWayPoint;
            if (timeWayPointDisplay > 86400)
                timeWayPointDisplay = timeWayPointDisplay - 86400;
            if (wp.isTarget() || (wp.getPrevNode() == null)) {
                //do nothing -> cut the first and the last
            } else if (wp.isReachedViaRoute()) {
                if(!(prevLinenr == "transferEdge")&&(!prevLinenr.equals(wp.getReachedBylineNr())))
                    System.out.println("Transfer");
                int timeDepartureDisplay = wp.getDepartureToThisPoint();
                if (timeDepartureDisplay > 86400)
                    timeDepartureDisplay = timeDepartureDisplay - 86400;
                System.out.println("Reach " + stoppingPointName + " at " + dataTransformer.makeTimeFromSeconds(timeWayPointDisplay) + " with line " + wp.getReachedBylineNr() + " (drive at: " + dataTransformer.makeTimeFromSeconds(timeDepartureDisplay) + ")");
            } else if (wp.getNode().getNodeType() == NodeType.CENTRAL_STOPPING_POINT) {
                System.out.println("Transfer");
            } else if (wp.getNode().getNodeType() == NodeType.STOPPING_POINT) {
                System.out.println("You are at " + stoppingPointName + " at " + dataTransformer.makeTimeFromSeconds(timeWayPointDisplay));
            }
            prevLinenr = wp.getReachedBylineNr();
        }
    }

    void start() throws Exception {
        String port = System.getenv("PORT");
        if (port == null || port.isEmpty()) {
            port = "8080";
        }

        String contextPath = "";
        String appBase = ".";

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(Integer.valueOf(port));
        tomcat.getHost().setAppBase(appBase);

        Context context = tomcat.addContext(contextPath, appBase);
        Tomcat.addServlet(context, JERSEY_SERVLET_NAME,
                new ServletContainer(new JerseyConfiguration()));
        context.addServletMappingDecoded("/api/*", JERSEY_SERVLET_NAME);

        tomcat.start();
        tomcat.getServer().await();
    }

    private static void printHelp() {
        System.out.println("needed files (next to jar) : \t-calendar.csv\n" +
                "\t-routes.csv\n" +
                "\t-trips.csv\n" +
                "\t-stops.csv\n" +
                "\t-stoptimes.csv\n" +
                "\t-calendar_dates.csv\n" +
                "\t-SEL_FZT_FELD.csv\n" +
                "\t-REC_ORT.csv\n" +
                "\t-REC_FRT.csv\n" +
                "\t-configDayData.yaml\n" +
                "\t-fileExceptionsStandardData.yaml\n" +
                "\t-fileExceptionsTestData.yaml\n" +
                "\t");
        System.out.println("-h / -help for help");
        System.out.println("-c / -conditions for checking the fifo conditions and if there are overlaps in location names -> write them in exception yaml");
        System.out.println("-p / -print to print the necessary algorithm steps and the path on the console");
        System.out.println("-s / -start, -t / -target, -d / -daytime are used for local execution");
        System.out.println("    example format: -s Dachauplatz -t Universitdt -d 07:30:00");
        System.out.println("-r / -rest to start a rest server, which can be used with this call (no other arguments considered except -help)");
        System.out.println("    server:port/api/shortestway/{identifierFrom}/{identifierTo}/{time}");
        System.out.println("    (example): http://localhost:8080/api/shortestway/1010/1020/00:00:00");
        System.out.println("");
        System.out.println("information about the location names and the associated identifier are found in nameIdentStandardData.txt and nameIdentTestData.txt");
        System.out.println("-createNameIdentFiles create the name identifier files (no other arguments considered and only local execution)");
        System.out.println("");
        System.out.println("-updateRoutingData to force using and update the current Dataset (has to be done if you change the day)");
        System.out.println("");
        System.out.println("configDayData.yaml configures which dataset is used and which day (ARGUMENTS LIKE -createNameIdentFiles ONLY WORK ON SPECIFIED DATA SET)");
        System.out.println("    if testdata: true -> write day of the year 04.05.2020 -> 20200504");
        System.out.println("    if testdata: false -> write weekday monday-sunday -> 1-7");
        System.out.println("");
        System.out.println("fileExceptionsStandardData.yaml / fileExceptionsTestData.yaml (standardData/testData) allows to...");
        System.out.println("    ...removeStoppingPointsIdentifiers -> remove stopping points with these identifiers");
        System.out.println("    ...transferExceptions -> change the transfer times of stopping points with these identifiers");
        System.out.println("    ...locationNameOverlaps -> rename stopping points with these identifiers");
    }
}