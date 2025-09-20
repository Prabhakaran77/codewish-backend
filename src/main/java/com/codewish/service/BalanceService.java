package com.codewish.service;

import com.codewish.model.GroupMember;
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

    @Autowired
    private GroupService groupService;

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

    public List<Settlement> getGroupSettlements(Long groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        List<Settlement> settlements = new ArrayList<>();

        // Calculate settlements between each pair of users
        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                GroupMember member1 = members.get(i);
                GroupMember member2 = members.get(j);

                BigDecimal balance1 = getUserBalanceInGroup(groupId, member1.getUser().getId());
                BigDecimal balance2 = getUserBalanceInGroup(groupId, member2.getUser().getId());

                // If one owes money and other should receive, create settlement
                if (balance1.compareTo(BigDecimal.ZERO) < 0 && balance2.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal settlementAmount = balance1.negate().min(balance2);
                    if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
                        settlements.add(new Settlement(
                                member1.getUser().getId(),
                                member1.getUser().getUsername(),
                                member2.getUser().getId(),
                                member2.getUser().getUsername(),
                                settlementAmount
                        ));
                    }
                } else if (balance2.compareTo(BigDecimal.ZERO) < 0 && balance1.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal settlementAmount = balance2.negate().min(balance1);
                    if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
                        settlements.add(new Settlement(
                                member2.getUser().getId(),
                                member2.getUser().getUsername(),
                                member1.getUser().getId(),
                                member1.getUser().getUsername(),
                                settlementAmount
                        ));
                    }
                }
            }
        }

        return settlements;
    }

    // Inner class for Settlement data
    public static class Settlement {
        private Long fromUserId;
        private String fromUsername;
        private Long toUserId;
        private String toUsername;
        private BigDecimal amount;

        public Settlement(Long fromUserId, String fromUsername, Long toUserId, String toUsername, BigDecimal amount) {
            this.fromUserId = fromUserId;
            this.fromUsername = fromUsername;
            this.toUserId = toUserId;
            this.toUsername = toUsername;
            this.amount = amount;
        }

        // Getters
        public Long getFromUserId() { return fromUserId; }
        public String getFromUsername() { return fromUsername; }
        public Long getToUserId() { return toUserId; }
        public String getToUsername() { return toUsername; }
        public BigDecimal getAmount() { return amount; }
    }
}