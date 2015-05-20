
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

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.dstadler.commons.logging.jdk.LoggerFactory;
import org.xml.sax.SAXException;

public class ChartsWriter {
    private final static Logger log = LoggerFactory.make();

    private static final FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd");

    public static void main(String[] args) throws IOException, SAXException, ParseException {
        LoggerFactory.initLogging();

//                    new URL("https://bz.apache.org/bugzilla/buglist.cgi?columnlist=product%2Ccomponent%2Cassigned_to%2Cbug_status%2Cresolution%2Cshort_desc%2Cchangeddate%2Cbug_id%2Copendate&f0=OP&f1=OP&f3=CP&f4=CP&j1=OR&list_id=122189&product=POI&query_format=advanced&ctype=rdf&human=1&limit=0"),
        try (InputStream stream = new FileInputStream("buglist2.xml")) {
            SortedMap<String, Map<String, String>> bugs =
                    new XmlHandler().parseContent(stream);
            log.info("Found " + bugs.size() + " entries");

            int open = 0;
            SortedMap<Date, BugStat> stats = new TreeMap<>();
            for(Map<String,String> bug : bugs.values()) {
                Date opened = getDate(bug, "opendate");

                if(isOpen(bug)) {
                	addOpened(stats, opened);
                	open++;
                }
            }

            write(bugs, stats, open);
        }
    }


	private static boolean isOpen(Map<String, String> bug) {
        String state = bug.get("bug_status");

        switch (state) {
            case "NEW":
            case "NEEDINFO":
            case "REOPENED":
            	return true;

            case "CLOSED":
            case "RESOLVED":
            case "VERIFIED":
            case "FIXED":
                return false;

            default:
                throw new IllegalArgumentException("Unexpected state: " + state);
        }
	}


	private static Date getDate(Map<String, String> bug, String attribute)
			throws ParseException {
		Date date;
		String dateStr = bug.get(attribute);
		
		if(dateStr == null) {
			return null;
		}
		
		if(dateStr.length() == 10) {
		       date = format.parse(dateStr);
		} else {
		    log.info("TODO: Date: " + dateStr + " for item '" + attribute + "', using " + new Date() + " for bug " + bug.get("id"));
		    date = new Date();
		}
		
		// only use day-resolution for now
		date = 
				DateUtils.setHours(
						DateUtils.setMinutes(
								DateUtils.setSeconds(
										DateUtils.setHours(date, 0), 0), 0), 0);
		
		return date;
	}


    private static void addOpened(SortedMap<Date, BugStat> stats, Date opened) {
        BugStat stat = stats.get(opened);
        if(stat == null) {
            stat = new BugStat();
            stats.put(opened, stat);
        }

        stat.stillOpen = stat.stillOpen + 1;
    }


    public static void write(Map<String, Map<String,String>> bugs, SortedMap<Date, BugStat> stats, int open) throws IOException, ParseException {
        Map<String,Object> context = new HashMap<>();
        context.put("bugs", bugs);
        context.put("writer", ChartsWriter.class);
        context.put("stats", stats);

        log.info("TODO: Writing to build/BugStats.html");

        Date lastWeek = DateUtils.addDays(new Date(), -7);
        int lastWeekOpened = 0, lastWeekTouched = 0, lastWeekClosed = 0;
        for(Map<String,String> bug : bugs.values()) {
        	Date date = getDate(bug, "opendate");
        	if(date == null) {
        		log.info("No opened-date for bug " + bug.get("id"));
        	} else if(date.after(lastWeek)) {
        		lastWeekOpened++;
        	}
        	Date changed = getDate(bug, "changeddate");
			if(changed != null && changed.after(lastWeek)) {
        		lastWeekTouched++;
        		if(!isOpen(bug)) {
        			lastWeekClosed++;
        		}
        	}
        }
        	
        log.info("Had: " + bugs.size() + " bugs, " + open + " are still open");
        log.info("Last week " + lastWeekOpened + " bugs were reported and " + lastWeekTouched + " were changed and up to " + lastWeekClosed + " were resolved");
        
        //VelocityUtils.render(context, "HeartratePlot.vm", new File("build/HeartratePlot.html"));
    }


}
