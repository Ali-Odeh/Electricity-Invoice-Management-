package Electricity.Management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import Electricity.Management.entity.User;
import Electricity.Management.exception.BadRequestException;
import Electricity.Management.exception.ResourceNotFoundException;
import Electricity.Management.repository.GeminiRepository;
import Electricity.Management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.url}")
    private String geminiUrl;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GeminiRepository geminiRepository;


    public Map<String, Object> processNaturalLanguageQuery(String query, Integer auditorUserId) {

        logger.info("Processing natural language query for auditor {}: {}", auditorUserId, query);

        User auditor = userRepository.findById(auditorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditor not found"));

        if (auditor.getProvider() == null) {
            throw new BadRequestException("Auditor must be assigned to a provider");
        }

        Integer providerId = auditor.getProvider().getProviderId();

        // Generate SQL using Gemini
        String sql = generateSQLFromNaturalLanguage(query, providerId);

        // Validate and execute SQL
        List<Map<String, Object>> results = geminiRepository.executeSql(sql);

        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("generatedSQL", sql);
        response.put("results", results);
        response.put("rowCount", results.size());

        logger.info("Query executed successfully, returned {} rows", results.size());
        return response;
    }


    private String generateSQLFromNaturalLanguage(String naturalLanguageQuery, Integer providerId) {
        try {
            String databaseSchema = getDatabaseSchema();

            String prompt = String.format(
                    "You are a SQL expert. Convert the following natural language question into a SQL query.\n\n" +
                            "Database Schema:\n%s\n\n" +
                            "Natural Language Question: %s\n\n" +
                            "CRITICAL SECURITY RULES - MUST FOLLOW:\n" +

                            "1. ONLY generate SELECT queries (no INSERT, UPDATE, DELETE, DROP, etc.)\n" +

                            "2. The auditor can ONLY access data for provider_id = %d\n" +

                            "3. MANDATORY filters for each table:\n" +
                            "   - User table: WHERE provider_id = %d\n" +
                            "   - Invoice table: WHERE provider_id = %d\n" +
                            "   - Pricing_History table: WHERE provider_id = %d\n" +
                            "   - Provider table: WHERE provider_id = %d\n" +
                            "   - Audit_logs: JOIN with Invoice and filter by Invoice.provider_id = %d\n" +

                            "4. NEVER query data without the provider_id filter\n" +
                            "5. If querying multiple tables, ensure ALL tables are filtered by provider_id\n" +
                            "6. Return ONLY the SQL query, no explanations or markdown\n" +
                            "7. Use proper JOINs when needed\n" +
                            "8. Limit results to 100 rows maximum\n" +

                            "SQL Query:",
                    databaseSchema, naturalLanguageQuery, providerId, providerId, providerId, providerId, providerId, providerId
            );

 /*                 "9. IMPORTANT: Always use explicit column names in SELECT, NEVER use * or A.*\n" +
                            "   Example: SELECT audit_id, invoice_id, action FROM Audit_logs , etc...\n" +
                            "   NOT: SELECT * FROM Audit_logs or SELECT A.* FROM Audit_logs AS A\n\n" +*/

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> parts = new HashMap<>();
            parts.put("text", prompt);
            content.put("parts", List.of(parts));
            requestBody.put("contents", List.of(content));

            String Gemini_fullUrl = geminiUrl + geminiApiKey;

            WebClient webClient = webClientBuilder.build();
            String response = webClient.post()
                    .uri(Gemini_fullUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse response
            JsonNode jsonNode = objectMapper.readTree(response);
            String generatedText = jsonNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // Clean up the SQL
            String sql = generatedText.trim()
                    .replaceAll("```sql", "")
                    .replaceAll("```", "")
                    .trim();

            logger.info("Generated SQL: {}", sql);
            return sql;

        } catch (Exception e) {
            logger.error("Failed to generate SQL from natural language: {}", e.getMessage());
            throw new BadRequestException("Failed to process query: " + e.getMessage());
        }
    }




    private String getDatabaseSchema() {
        return """
            Tables:

            1. Provider (provider_id, name, city, email, phone_number, current_kwh_price, created_at, updated_at, is_active)

            2. User (user_id, provider_id, name, email, address, phone_number, created_at, updated_at, role)
               - role: 'Customer', 'Invoice_Creator', 'Super_Creator', 'Auditor', 'Admin'

            3. Invoice (invoice_id, customer_id, provider_id, created_by_user_id, pricing_id, invoice_number,
                        kwh_consumed, total_amount, issue_date, due_date, payment_status, payment_date, created_at)
               - payment_status: 'Pending', 'Paid', 'Overdue', 'Cancelled'

            4. Pricing_History (pricing_id, provider_id, changed_by_user_id, kwh_price, valid_from, valid_to, created_at)

            5. Audit_logs (audit_id, invoice_id, performed_by_user_id, action, old_value, new_value, performed_at)
               - action: 'create', 'update', 'delete'
               
            __________________
            
                Relationships Summary:
                
                Provider (1) ➜ User (N)               Each provider can have multiple users.
                Provider (1) ➜ Invoice (N)            Each provider can issue multiple invoices.
                Provider (1) ➜ Pricing_History (N)    Each provider has its own pricing history.
                
                User (1) ➜ Invoice (N)                Each customer can have multiple invoices.
                User (1) ➜  Invoice (N)               Each employee (creator) can create multiple invoices.
                
                User (1) ➜ Audit_Logs (N)             Each user can perform multiple actions (create/update invoices).
                User (1) ➜ Pricing_History (N)        Each administrator can change their provider's rates multiple times.
                
                Invoice (1) ➜ Audit_Logs (N)          Each invoice can have multiple audit log records.
                Pricing_History (1) ➜ Invoice (N)     Each invoice references the price that was active at its issuance.
                
            """;
    }
}
