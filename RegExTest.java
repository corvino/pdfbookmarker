import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExTest {
    public static void main(String args[]) throws Exception
    {
        if (2 != args.length) {
            System.err.println("Usage: <regex> <content>");
        } else {
            String regex = args[0];
            String content = args[1];
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);
            boolean result = matcher.find();

            if (!result) {
                System.out.println("Did not match!");
            } else {
                System.out.println("Matched with " + matcher.groupCount() + " groups:");
                System.out.println("  Entire Match: '" + matcher.group(0) + "'");

                for (int i = 1; i <= matcher.groupCount(); i++) {
                    System.out.println("  " + i + " : '" + matcher.group(i) + "'");
                }
            }
        }
    }
}
