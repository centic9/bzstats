import java.util.Map;



public class BugStat {
    public int stillOpen;
    public int closedToday;

	public static boolean isOpen(Map<String, String> bug) {
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
	
	public static boolean isNeedinfo(Map<String, String> bug) {
		return bug.get("bug_status").equals("NEEDINFO");
	}
}
