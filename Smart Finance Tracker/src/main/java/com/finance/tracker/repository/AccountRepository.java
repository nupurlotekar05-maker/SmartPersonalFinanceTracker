
package com.finance.tracker.repository;

import com.finance.tracker.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);
}
