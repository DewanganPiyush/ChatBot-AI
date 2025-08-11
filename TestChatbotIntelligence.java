import java.util.*;

public class TestChatbotIntelligence {
    public static void main(String[] args) {
        // Test the employee database and fuzzy matching
        Map<String, EmployeeInfo> EMPLOYEE_DATABASE = new HashMap<>();
        
        // Initialize test data
        addEmployee("Saikumar Niletam", new String[]{"saikumar", "sai", "kumar", "niletam", "sai kumar", "saykumar", "saikumar niletam"}, 
                   "Talent Acquisition Specialist", "General & Administrative");
        
        addEmployee("Meghana Karimerakala", new String[]{"meghana", "meghan", "megan", "meghna", "karimerakala", "megana"}, 
                   "People Operations, Associate", "General & Administrative");
        
        // Test cases
        String[] testQueries = {"sai", "megan", "piyus", "jay"};
        
        for (String query : testQueries) {
            System.out.println("Testing query: '" + query + "'");
            String suggestion = findEmployeeSuggestion(query, EMPLOYEE_DATABASE);
            System.out.println("Suggestion: " + suggestion);
            System.out.println("---");
        }
    }
    
    private static void addEmployee(String fullName, String[] variations, String role, String department) {
        EmployeeInfo emp = new EmployeeInfo(fullName, role, department, variations);
        EMPLOYEE_DATABASE.put(fullName.toLowerCase(), emp);
        
        for (String variation : variations) {
            EMPLOYEE_DATABASE.put(variation.toLowerCase(), emp);
        }
    }
    
    private static String findEmployeeSuggestion(String query, Map<String, EmployeeInfo> database) {
        List<EmployeeMatch> matches = new ArrayList<>();
        
        for (EmployeeInfo emp : new HashSet<>(database.values())) {
            int score = calculateEmployeeMatchScore(query, emp);
            if (score > 0) {
                matches.add(new EmployeeMatch(emp.fullName, score));
            }
        }
        
        matches.sort((a, b) -> Integer.compare(b.score, a.score));
        
        if (!matches.isEmpty()) {
            return "Did you mean " + matches.get(0).name + "?";
        }
        return "No suggestions found";
    }
    
    private static int calculateEmployeeMatchScore(String query, EmployeeInfo emp) {
        String lowerQuery = query.toLowerCase();
        int score = 0;
        
        for (String variation : emp.variations) {
            if (variation.equals(lowerQuery)) {
                return 100;
            }
            if (variation.contains(lowerQuery) || lowerQuery.contains(variation)) {
                score += 50;
            }
        }
        
        return score;
    }
    
    static class EmployeeInfo {
        String fullName;
        String role;
        String department;
        String[] variations;

        EmployeeInfo(String fullName, String role, String department, String[] variations) {
            this.fullName = fullName;
            this.role = role;
            this.department = department;
            this.variations = variations;
        }
    }
    
    static class EmployeeMatch {
        String name;
        int score;

        EmployeeMatch(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }
    
    private static Map<String, EmployeeInfo> EMPLOYEE_DATABASE = new HashMap<>();
}
