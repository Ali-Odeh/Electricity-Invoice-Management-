package Electricity.Management.repository;

import Electricity.Management.Enum.Role;
import Electricity.Management.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUser_UserId(Integer userId);

    Optional<UserRole> findByUser_UserIdAndRole(Integer userId, Role role);

    @Query("SELECT ur.role FROM UserRole ur WHERE ur.user.userId = :userId")
    List<Role> findRolesByUserId(Integer userId);

    void deleteByUser_UserIdAndRole(Integer userId, Role role);

    boolean existsByUser_UserIdAndRole(Integer userId, Role role);
}
