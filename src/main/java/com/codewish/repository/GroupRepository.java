package com.codewish.repository;

import com.codewish.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByCreatedBy(Long createdBy);

    @Query("SELECT g FROM Group g JOIN g.members gm WHERE gm.user.id = :userId")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);
}
