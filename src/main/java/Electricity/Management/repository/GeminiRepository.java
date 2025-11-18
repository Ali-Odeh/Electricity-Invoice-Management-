package Electricity.Management.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class GeminiRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Executes a native SQL SELECT query safely and returns results as Map.
     * Only SELECT statements are allowed; any modification queries will be rejected.
     *
     * @param sql The SQL query to execute.
     * @return List of result rows (each row as Map<String, Object>).
     * @throws IllegalArgumentException if the query is null or not a SELECT statement.
     */

    @SuppressWarnings("unchecked")
    @Transactional
    public List<Map<String, Object>> executeSql(String sql) {
        if (sql == null || !sql.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException(
                    "Query must be a SELECT statement. Only SELECT queries are permitted."
            );
        }

        Query query = entityManager.createNativeQuery(sql);

        NativeQuery<Map<String, Object>> nativeQuery = query.unwrap(NativeQuery.class);
        nativeQuery.setTupleTransformer((tuple, aliases) -> {
            Map<String, Object> result = new java.util.HashMap<>();
            for (int i = 0; i < aliases.length; i++) {
                result.put(aliases[i], tuple[i]);
            }
            return result;
        });
        
        return nativeQuery.getResultList();
    }

}
