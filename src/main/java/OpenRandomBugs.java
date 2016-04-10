import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dstadler.commons.logging.jdk.LoggerFactory;
import org.dstadler.commons.util.DocumentStarter;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Logger;

/**
 * Open a number of open POI bugs randomly to let us randomly work on
 * some interesting stuff.
 */
public class OpenRandomBugs {
    private final static Logger log = LoggerFactory.make();

    public static void main(String[] args) throws IOException, SAXException, InterruptedException {
        LoggerFactory.initLogging();

        log.info("Fetching data from " + POIBugzillaStats.URL);
        try (InputStream stream = POIBugzillaStats.URL.openStream()) {
            //try (InputStream stream = new FileInputStream("buglist2.xml")) {
            SortedMap<String, Map<String, String>> bugs =
                    new XmlHandler().parseContent(stream);
            log.info("Found " + bugs.size() + " entries");

            List<Pair<String, Map<String, String>>> bugList = new ArrayList<>();

            //bugs.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).;
            for(Map.Entry<String, Map<String,String>> entry : bugs.entrySet()) {
                if(!BugStat.isOpen(entry.getValue())) {
                    continue;
                }

                bugList.add(Pair.of(entry.getKey(), entry.getValue()));
            }

            log.info("Found " + bugList.size() + " open bugs");

            DocumentStarter starter = new DocumentStarter();
            for(int i = 0;i < 5;i++) {
                int randomIndex = RandomUtils.nextInt(bugList.size());
                Pair<String, Map<String, String>> entry = bugList.get(randomIndex);
                starter.openURL(POIBugzillaStats.BASE_URL + "show_bug.cgi?id=" + entry.getValue().get("id"));

                Thread.sleep(1000);
            }
        }
    }
}
