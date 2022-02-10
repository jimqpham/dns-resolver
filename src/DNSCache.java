import java.util.HashMap;

public class DNSCache {

    private static final HashMap<DNSQuestion, DNSRecord> lookupTable = new HashMap<>();

    /**
     * Look up the cache
     * @param question DNSQuestion object to look up
     * @return a valid record or null if an expired record or no record is found
     */
    public static DNSRecord lookup(DNSQuestion question) {
        DNSRecord answer = lookupTable.get(question);
        if (answer == null) {
            System.out.println("\tAnswer to " + question.getDomainNameAsString() + " not in cache.");
            return null;
        }
        else if (!answer.timestampValid()) {
            System.out.println("\tAnswer to " + question.getDomainNameAsString() + " expired.");
            lookupTable.remove(question);
            return null;
        }
        else {
            System.out.println("\tAnswer to " + question.getDomainNameAsString() + " found in cache.");
            return answer;
        }
    }

    /**
     * Insert a new pair of question-record into the cache
     * @param question The DNS Question to insert
     * @param record The DNS Record object that answers the question
     */
    public static void insert(DNSQuestion question, DNSRecord record) {
        System.out.println("\tStoring " + question.getDomainNameAsString() + " in cache...");
        lookupTable.put(question, record);
    }
}
