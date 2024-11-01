import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.dstadler.commons.collections.MappedCounter;
import org.dstadler.commons.collections.MappedCounterImpl;
import org.dstadler.commons.logging.jdk.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Fetch bugzilla stats as XML and combine them into statistics that are
 * collected in file "stats.csv".
 */
public class POIBugzillaStats {
    public final static String BASE_URL = "https://bz.apache.org/bugzilla/";

	public final static URL URL;
    static {
        try {
            URL = new URL(BASE_URL + "buglist.cgi?columnlist="
                    + "product%2C"
                    + "component%2C"
                    + "assigned_to%2C"
                    + "bug_status%2C"
                    + "resolution%2C"
                    + "short_desc%2C"
                    + "changeddate%2C"
                    + "bug_id%2C"
                    + "bug_severity%2C"
                    + "priority%2C"
                    + "keywords%2C"
                    + "opendate&f0=OP&f1=OP&f3=CP&f4=CP&j1=OR&list_id=122189&product=POI&" +
                    "query_format=advanced&ctype=rdf&human=1&limit=0");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private final static Logger log = LoggerFactory.make();

    private static final FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd");
    private static final FastDateFormat outformat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");

    private static final File FILE = new File("stats.csv");

    public static void main(String[] args) throws IOException, SAXException, ParseException {
        LoggerFactory.initLogging();

        log.info("Fetching data from " + URL);
        try (InputStream stream = URL.openStream()) {
            SortedMap<String, Map<String, String>> bugs =
                    new XmlHandler().parseContent(stream);
            log.info("Found " + bugs.size() + " entries");

            int open = 0;
            SortedMap<Date, BugStat> stats = new TreeMap<>();
            for (Map<String, String> bug : bugs.values()) {
                Date opened = getDate(bug, "opendate");

                if (BugStat.isOpen(bug)) {
                    addOpened(stats, opened);
                    open++;
                }
            }

            write(bugs, open);
        }
    }


    private static Date getDate(Map<String, String> bug, String attribute)
            throws ParseException {
        Date date;
        String dateStr = bug.get(attribute);

        if (dateStr == null) {
            return null;
        }

        if (dateStr.length() == 10) {
            date = format.parse(dateStr);
        } else {
			// some dates are reported relative to now, e.g. "Fri. 01:40", we use current date instead for now
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
        //noinspection Java8ReplaceMapGet
        if (stat == null) {
            stat = new BugStat();
            stats.put(opened, stat);
        }

        stat.stillOpen = stat.stillOpen + 1;
    }

    public static void write(Map<String, Map<String, String>> bugs, int open) throws IOException, ParseException {
        MappedCounter<String> components = new MappedCounterImpl<>();

        Date lastWeek = DateUtils.addDays(new Date(), -7);
        int lastWeekOpened = 0, lastWeekTouched = 0, lastWeekClosed = 0;
        int enhancement = 0, patch = 0, needinfo = 0;
        for (Map<String, String> bug : bugs.values()) {
            Date date = getDate(bug, "opendate");
            if (date == null) {
                log.info("No opened-date for bug " + bug.get("id"));
            } else if (date.after(lastWeek)) {
                lastWeekOpened++;
            }
            Date changed = getDate(bug, "changeddate");
            if (changed != null && changed.after(lastWeek)) {
                lastWeekTouched++;
                if (!BugStat.isOpen(bug)) {
                    lastWeekClosed++;
                }
            }

            // look at open bugs only for number of enhancements, patches, needinfo, ...
            if (!BugStat.isOpen(bug)) {
                continue;
            }

            if (getSeverity(bug) == BugSeverity.enhancement) {
                enhancement++;
            } else if (BugStat.isNeedinfo(bug)) {
                needinfo++;
            } else {
                components.inc(bug.get("component"));

                if (getKeywords(bug).toLowerCase().contains("patch")) {
                    patch++;
                }
            }
        }

        String output = "Result:\n" +
                "    " + outformat.format(new Date()) + "     Had: " + bugs.size() + " bugs reported for POI altogether\n" +
                "    " + outformat.format(new Date()) + "     " + open + " bugs are open overall\n" +
                "    " + outformat.format(new Date()) + "     Having " + enhancement + " enhancements\n" +
                "    " + outformat.format(new Date()) + "     Thus having " + (open - enhancement) + " actual bugs\n" +
                "    " + outformat.format(new Date()) + "     " + needinfo + " of these are waiting for feedback\n" +
                "    " + outformat.format(new Date()) + "     Thus having " + (open - enhancement - needinfo) + " actual workable bugs\n" +
                "    " + outformat.format(new Date()) + "     " + patch + " of the workable bugs have patches available\n" +
                "    " + outformat.format(new Date()) + "     Distribution of workable bugs across components: " + components.sortedMap() + "\n" +
                "    " + outformat.format(new Date()) + "     Last week " + lastWeekOpened + " new bugs were reported and " + lastWeekTouched + " were changed and up to " + lastWeekClosed + " were resolved\n";
        log.info(output);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE, true))) {
            // Date,Timestamp,Bugs overall,Open overall,Enhancements,Actual bugs,Needinfo,Workable bugs,Bugs with patch,Opened last week,Changed last week,Closed last week,Distribution
            writer.write(outformat.format(new Date()) + "," + new Date().getTime() + "," +
                    bugs.size() + "," + open + "," + enhancement + "," + (open - enhancement) + "," + needinfo + "," + (open - enhancement - needinfo) + "," +
                    patch + "," + lastWeekOpened + "," + lastWeekTouched + "," + lastWeekClosed + "," +
                    components.sortedMap().toString().replace(",", ";") +
                    "\n");
        }
    }

    private static String getKeywords(Map<String, String> bug) {
        return bug.get("keywords");
    }

    private static BugSeverity getSeverity(Map<String, String> bug) {
        return BugSeverity.valueOf(bug.get("bug_severity"));
    }
}
