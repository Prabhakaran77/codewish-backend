package com.codewish.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "expense_splits",
        uniqueConstraints = @UniqueConstraint(columnNames = {"expense_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount_owed", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountOwed;

    public ExpenseSplit(Expense expense, Long userId, BigDecimal amountOwed) {
        this.expense = expense;
        this.userId = userId;
        this.amountOwed = amountOwed;
    }
}
