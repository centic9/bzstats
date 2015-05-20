
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.FastDateFormat;
import org.dstadler.commons.logging.jdk.LoggerFactory;
import org.xml.sax.SAXException;

public class ChartsWriter {
    private final static Logger log = LoggerFactory.make();

    private static final FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd");

    public static void main(String[] args) throws IOException, SAXException, ParseException {
        LoggerFactory.initLogging();

//                    new URL("https://bz.apache.org/bugzilla/buglist.cgi?columnlist=product%2Ccomponent%2Cassigned_to%2Cbug_status%2Cresolution%2Cshort_desc%2Cchangeddate%2Cbug_id%2Copendate&f0=OP&f1=OP&f3=CP&f4=CP&j1=OR&list_id=122189&product=POI&query_format=advanced&ctype=rdf&human=1&limit=0"),
        try (InputStream stream = new FileInputStream("buglist.xml")) {
            SortedMap<String, Map<String, String>> bugs =
                    new XmlHandler().parseContent(stream);
            log.info("Found " + bugs.size() + " entries");

            SortedMap<Date, BugStat> stats = new TreeMap<>();
            for(Map<String,String> bug : bugs.values()) {
                final Date opened;
                String openDateStr = bug.get("opendate");
                if(openDateStr.length() == 10) {
                       opened = format.parse(openDateStr);
                } else {
                    log.info("TODO: Date: " + openDateStr);
                    opened = new Date();
                }
                String state = bug.get("bug_status");

                switch (state) {
                    case "NEW":
                    case "NEEDINFO":
                    case "REOPENED":
                        addOpened(stats, opened);
                        break;

                    case "CLOSED":
                    case "RESOLVED":
                    case "VERIFIED":
                    case "FIXED":
                        break;

                    default:
                        throw new IllegalArgumentException("Unexpected state: " + state);
                }
            }

            write(bugs);
        }
    }


    private static void addOpened(SortedMap<Date, BugStat> stats, Date opened) {
        BugStat stat = stats.get(opened);
        if(stat == null) {
            stat = new BugStat();
            stats.put(opened, stat);
        }

        stat.stillOpen = stat.stillOpen + 1;
    }


    public static void write(Map<String, Map<String,String>> bugs) throws IOException {
        Map<String,Object> context = new HashMap<>();
        context.put("bugs", bugs);
        context.put("writer", ChartsWriter.class);

        log.info("TODO: Writing to build/BugStats.html");
        //VelocityUtils.render(context, "HeartratePlot.vm", new File("build/HeartratePlot.html"));
    }


}
