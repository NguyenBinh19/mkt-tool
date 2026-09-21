package com.example.extracttool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.extracttool.model.entity.Comment;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    boolean existsByPostUrlAndFbId(String postUrl, String fbId);
}
