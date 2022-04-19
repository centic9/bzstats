import java.util.Map;

/**
 * Helper to parse bug-status and decide if a bug is open or in state "needinfo"
 */
public class BugStat {
    public int stillOpen;

	public static boolean isOpen(Map<String, String> bug) {
        String state = bug.get("bug_status");

        switch (state) {
            case "NEW":
            case "NEEDINFO":
            case "REOPENED":
            case "UNCONFIRMED":
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

	public static boolean isNeedinfo(Map<String, String> bug) {
		return bug.get("bug_status").equals("NEEDINFO");
	}
}
