import tools.jackson.databind.JsonNode;

/**
 * Helper to parse bug-status and decide if a bug is open or in state "needinfo"
 */
public class BugStat {
    public int stillOpen;

	public static boolean isOpen(JsonNode bug) {
        String state = bug.get("status").asString();

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

	public static boolean isNeedinfo(JsonNode bug) {
		return bug.get("status").asString().equals("NEEDINFO");
	}
}
