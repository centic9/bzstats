import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.lang3.RandomUtils;
import org.dstadler.commons.http5.HttpClientWrapper5;
import org.dstadler.commons.logging.jdk.LoggerFactory;
import org.dstadler.commons.util.DocumentStarter;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Open a number of open POI bugs randomly to let us randomly work on
 * some interesting stuff.
 */
public class OpenRandomBugs {
    private final static Logger log = LoggerFactory.make();

	private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException, SAXException, InterruptedException {
        LoggerFactory.initLogging();

		log.info("Fetching data from " + POIBugzillaStats.URL);
		String ret = HttpClientWrapper5.retrieveData(POIBugzillaStats.URL.toString());

		JsonNode jsonNode = objectMapper.readTree(ret).get("bugs");
		log.info("Found " + jsonNode.size() + " entries");

		DocumentStarter starter = new DocumentStarter();
		for(int i = 0;i < 5;i++) {
			int randomIndex = RandomUtils.insecure().randomInt(0, jsonNode.size());
			JsonNode entry = jsonNode.get(randomIndex);
			starter.openURL(POIBugzillaStats.BASE_URL + "show_bug.cgi?id=" + entry.get("id").asInt());

			Thread.sleep(2000);
		}
    }
}
