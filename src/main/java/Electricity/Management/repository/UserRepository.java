package Electricity.Management.repository;

import Electricity.Management.Enum.UserRole;
import Electricity.Management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email); // to Avoid NullPointerException

    boolean existsByEmail(String email);

}
