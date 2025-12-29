import tools.jackson.databind.JsonNode;

/**
 * Helper to parse bug-status and decide if a bug is open or in state "needinfo"
 */
public class BugStat {
	public static boolean isOpen(JsonNode bug) {
        String state = bug.get("status").asString();

		return switch (state) {
			case "NEW", "NEEDINFO", "REOPENED", "UNCONFIRMED" -> true;
			case "CLOSED", "RESOLVED", "VERIFIED", "FIXED" -> false;
			default -> throw new IllegalArgumentException("Unexpected state: " + state);
		};
	}

	public static boolean isNeedinfo(JsonNode bug) {
		return bug.get("status").asString().equals("NEEDINFO");
	}
}
