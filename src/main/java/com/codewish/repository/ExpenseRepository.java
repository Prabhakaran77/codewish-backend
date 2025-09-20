package com.codewish.repository;

import com.codewish.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupId(Long groupId);
    List<Expense> findByPaidByUserId(Long paidByUserId);

    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId ORDER BY e.expenseDate DESC")
    List<Expense> findByGroupIdOrderByDateDesc(@Param("groupId") Long groupId);
}
