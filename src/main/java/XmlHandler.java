import java.util.HashMap;
import java.util.Map;

import org.dstadler.commons.xml.AbstractSimpleContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;

/**
 * Parser for the Bugzilla XML format.
 *
 * Returns a map of issues with a corresponding map of key-value pairs.
 */
public class XmlHandler extends AbstractSimpleContentHandler<String, Map<String,String>> {
    private boolean inBug = false;
    private Map<String,String> tags;

    /*<bz:bug rdf:about="https://bz.apache.org/bugzilla/show_bug.cgi?id=45124">

          <bz:id nc:parseType="Integer">45124</bz:id>

          <bz:product>POI</bz:product>
          <bz:component>HSSF</bz:component>
          <bz:assigned_to>dev</bz:assigned_to>
          <bz:bug_status>NEW</bz:bug_status>
          <bz:resolution></bz:resolution>
          <bz:short_desc>inserting text or images wipes out boldness and makes everything italic</bz:short_desc>
          <bz:changeddate>2011-05-28</bz:changeddate>
          <bz:opendate>2008-06-03</bz:opendate>

        </bz:bug>*/

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if(localName.equals("bug")) {
            Preconditions.checkState(!inBug, "Should not have nested 'bug' tags");
            inBug = true;
            tags = new HashMap<>();
        }
        super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (localName.equals("bug")) {
            String bugId = tags.get("id");
            //log.info("Found bug " + bugId);
            configs.put(bugId, tags);
            inBug = false;
        } else if (inBug) {
            tags.put(localName, characters.toString().trim());
        }
        characters.setLength(0);
    }
}
