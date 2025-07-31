import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.dstadler.commons.collections.MappedCounter;
import org.dstadler.commons.collections.MappedCounterImpl;
import org.dstadler.commons.http5.HttpClientWrapper5;
import org.dstadler.commons.logging.jdk.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Fetch bugzilla stats as XML and combine them into statistics that are
 * collected in file "stats.csv".
 */
public class POIBugzillaStats {
    public final static String BASE_URL = "https://bz.apache.org/bugzilla/";

	public final static URL URL;
    static {
        try {
            URL = new URL(BASE_URL + "rest.cgi/bug?"
					+ "api_key=" + System.getenv("BUGZILLA_API_KEY") + "&"
					+ "product=POI&"
					+ "status=UNCONFIRMED&status=NEW&status=ASSIGNED&status=REOPENED&status=NEEDINFO");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private final static Logger log = LoggerFactory.make();

	private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd");
    private static final FastDateFormat outformat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");

    private static final File FILE = new File("stats.csv");

    public static void main(String[] args) throws IOException, ParseException {
        LoggerFactory.initLogging();

		log.info("Fetching data from " + URL);
		String ret = HttpClientWrapper5.retrieveData(URL.toString());

		/*Files.copy(new ByteArrayInputStream(ret.getBytes(StandardCharsets.UTF_8)),
				Path.of("/tmp", "test.json"), StandardCopyOption.REPLACE_EXISTING);*/

		JsonNode jsonNode = objectMapper.readTree(ret).get("bugs");
		log.info("Found " + jsonNode.size() + " entries");

		write(jsonNode);
    }

    private static Date getDate(JsonNode bug, String attribute)
            throws ParseException {
        Date date;
        String dateStr = Objects.requireNonNull(bug.get(attribute),
				"No data found for attribute " + attribute + " for " + bug).asText();

        if (dateStr == null) {
            return null;
        }

        if (dateStr.length() == 10) {
			date = format.parse(dateStr);
		} else if  (dateStr.length() == 20) {
			// "2010-05-20T04:40:12Z"
			date = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse(dateStr);
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

    public static void write(JsonNode bugs) throws IOException, ParseException {
        MappedCounter<String> components = new MappedCounterImpl<>();

        Date lastWeek = DateUtils.addDays(new Date(), -7);
        int lastWeekOpened = 0, lastWeekTouched = 0, lastWeekClosed = 0;
        int enhancement = 0, patch = 0, needinfo = 0;
        for (JsonNode bug : bugs) {
            Date date = getDate(bug, "creation_time");
            if (date == null) {
                log.info("No opened-date for bug " + bug.get("id"));
            } else if (date.after(lastWeek)) {
                lastWeekOpened++;
            }
            Date changed = getDate(bug, "last_change_time");
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
                components.inc(bug.get("component").asText());

                if (getKeywords(bug).toLowerCase().contains("patch")) {
                    patch++;
                }
            }
        }

		int open = bugs.size();
		String output = "Result:\n" +
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
					open + "," + open + "," + enhancement + "," + (open - enhancement) + "," + needinfo + "," + (open - enhancement - needinfo) + "," +
                    patch + "," + lastWeekOpened + "," + lastWeekTouched + "," + lastWeekClosed + "," +
                    components.sortedMap().toString().replace(",", ";") +
                    "\n");
        }
    }

    private static String getKeywords(JsonNode bug) {
        return bug.get("keywords").asText();
    }

    private static BugSeverity getSeverity(JsonNode bug) {
        return BugSeverity.valueOf(bug.get("severity").asText());
    }
}
