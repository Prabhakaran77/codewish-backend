package com.codewish.repository;

import com.codewish.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {
    List<ExpenseSplit> findByExpenseId(Long expenseId);
    List<ExpenseSplit> findByUserId(Long userId);

    @Query("SELECT es FROM ExpenseSplit es WHERE es.expense.group.id = :groupId AND es.userId = :userId")
    List<ExpenseSplit> findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT SUM(es.amountOwed) FROM ExpenseSplit es WHERE es.expense.group.id = :groupId AND es.userId = :userId")
    BigDecimal getTotalOwedByUserInGroup(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.group.id = :groupId AND e.paidByUserId = :userId")
    BigDecimal getTotalPaidByUserInGroup(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
