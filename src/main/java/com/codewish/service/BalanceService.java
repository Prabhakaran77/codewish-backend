package com.codewish.service;

import com.codewish.repository.ExpenseSplitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map;

@Service
public class BalanceService {

    @Autowired
    private ExpenseSplitRepository expenseSplitRepository;

    public BigDecimal getUserBalanceInGroup(Long groupId, Long userId) {
        // Amount user owes
        BigDecimal totalOwed = expenseSplitRepository.getTotalOwedByUserInGroup(groupId, userId);
        if (totalOwed == null) totalOwed = BigDecimal.ZERO;

        // Amount user paid
        BigDecimal totalPaid = expenseSplitRepository.getTotalPaidByUserInGroup(groupId, userId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        // Net balance: negative means user owes money, positive means user should receive money
        return totalPaid.subtract(totalOwed);
    }

    public Map<Long, BigDecimal> getAllBalancesInGroup(Long groupId, List<Long> userIds) {
        Map<Long, BigDecimal> balances = new HashMap<>();
        for (Long userId : userIds) {
            balances.put(userId, getUserBalanceInGroup(groupId, userId));
        }
        return balances;
    }
}
