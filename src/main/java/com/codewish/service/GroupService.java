package com.codewish.service;

import com.codewish.model.Group;
import com.codewish.model.GroupMember;
import com.codewish.model.User;
import com.codewish.repository.GroupRepository;
import com.codewish.repository.GroupMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Transactional
    public Group createGroup(String name, String description, Long createdBy) {
        Group group = new Group(name, description, createdBy);
        Group savedGroup = groupRepository.save(group);

        // Add creator as group member
        User creator = new User();
        creator.setId(createdBy);
        GroupMember creatorMember = new GroupMember(creator, savedGroup);
        groupMemberRepository.save(creatorMember);

        return savedGroup;
    }

    public Optional<Group> findById(Long id) {
        return groupRepository.findById(id);
    }

    public List<Group> findGroupsByUserId(Long userId) {
        return groupRepository.findGroupsByUserId(userId);
    }

    public List<Group> findGroupsCreatedByUser(Long userId) {
        return groupRepository.findByCreatedBy(userId);
    }

    @Transactional
    public boolean addUserToGroup(Long groupId, Long userId) {
        if (groupMemberRepository.existsByUserIdAndGroupId(userId, groupId)) {
            return false; // User already in group
        }

        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isPresent()) {
            User user = new User();
            user.setId(userId);
            GroupMember member = new GroupMember(user, groupOpt.get());
            groupMemberRepository.save(member);
            return true;
        }
        return false;
    }

    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }

    @Transactional
    public void removeUserFromGroup(Long groupId, Long userId) {
        groupMemberRepository.deleteByUserIdAndGroupId(userId, groupId);
    }
}

