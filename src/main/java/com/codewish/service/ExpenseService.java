package com.codewish.service;

import com.codewish.model.Expense;
import com.codewish.model.ExpenseSplit;
import com.codewish.model.Group;
import com.codewish.model.GroupMember;
import com.codewish.repository.ExpenseRepository;
import com.codewish.repository.ExpenseSplitRepository;
import com.codewish.repository.GroupMemberRepository;
import com.codewish.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseSplitRepository expenseSplitRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Transactional
    public Expense createExpenseWithEqualSplit(Long groupId, String description, BigDecimal amount,
                                               Long paidByUserId, LocalDate expenseDate) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (!groupOpt.isPresent()) {
            throw new RuntimeException("Group not found");
        }

        Group group = groupOpt.get();
        Expense expense = new Expense(group, description, amount, paidByUserId, expenseDate);
        Expense savedExpense = expenseRepository.save(expense);

        // Create equal splits for all group members
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        BigDecimal splitAmount = amount.divide(new BigDecimal(members.size()), 2, RoundingMode.HALF_UP);

        for (GroupMember member : members) {
            ExpenseSplit split = new ExpenseSplit(savedExpense, member.getUser().getId(), splitAmount);
            expenseSplitRepository.save(split);
        }

        return savedExpense;
    }

    @Transactional
    public Expense createExpenseWithCustomSplit(Long groupId, String description, BigDecimal amount,
                                                Long paidByUserId, LocalDate expenseDate, List<Long> participantIds) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (!groupOpt.isPresent()) {
            throw new RuntimeException("Group not found");
        }

        if (participantIds == null || participantIds.isEmpty()) {
            throw new RuntimeException("At least one participant is required");
        }

        Group group = groupOpt.get();
        Expense expense = new Expense(group, description, amount, paidByUserId, expenseDate);
        Expense savedExpense = expenseRepository.save(expense);

        // Create equal splits only for selected participants
        BigDecimal splitAmount = amount.divide(new BigDecimal(participantIds.size()), 2, RoundingMode.HALF_UP);

        for (Long participantId : participantIds) {
            ExpenseSplit split = new ExpenseSplit(savedExpense, participantId, splitAmount);
            expenseSplitRepository.save(split);
        }

        return savedExpense;
    }
    public List<Expense> getGroupExpenses(Long groupId) {
        return expenseRepository.findByGroupIdOrderByDateDesc(groupId);
    }

    public Optional<Expense> findById(Long id) {
        return expenseRepository.findById(id);
    }

    @Transactional
    public void createSettlementExpense(Long groupId, Long fromUserId, Long toUserId, BigDecimal amount) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (!groupOpt.isPresent()) {
            throw new RuntimeException("Group not found");
        }

        Group group = groupOpt.get();

        // Create settlement expense - person who owes money "pays" the settlement
        Expense settlementExpense = new Expense(group, "Settlement", amount, fromUserId, java.time.LocalDate.now());
        Expense savedExpense = expenseRepository.save(settlementExpense);

        // Create split where only the person who should receive money "owes" the settlement
        // This effectively transfers the debt
        ExpenseSplit split = new ExpenseSplit(savedExpense, toUserId, amount);
        expenseSplitRepository.save(split);
    }

    public List<ExpenseSplit> getExpenseSplits(Long expenseId) {
        return expenseSplitRepository.findByExpenseId(expenseId);
    }
}