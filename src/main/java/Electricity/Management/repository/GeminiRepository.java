package Electricity.Management.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class GeminiRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Executes a native SQL SELECT query safely.
     * Only SELECT statements are allowed; any modification queries will be rejected.
     *
     * @param sql The SQL query to execute.
     * @return List of result rows (each row as Object[]).
     * @throws IllegalArgumentException if the query is null or not a SELECT statement.
     */

    @SuppressWarnings("unchecked")
    @Transactional
    public List<Object[]> executeSql(String sql) {
        if (sql == null || !sql.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException(
                    "Query must be a SELECT statement. Only SELECT queries are permitted."
            );
        }

        return entityManager.createNativeQuery(sql).getResultList();
    }

}
