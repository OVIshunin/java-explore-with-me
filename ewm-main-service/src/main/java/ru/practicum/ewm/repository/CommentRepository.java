package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.author " +
            "WHERE c.event.id = :eventId AND c.status = :status " +
            "ORDER BY c.createdOn DESC")
    List<Comment> findByEventIdAndStatus(@Param("eventId") Long eventId,
                                         @Param("status") CommentStatus status,
                                         Pageable pageable);

     @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.author " +
            "JOIN FETCH c.event " +
            "WHERE c.status = :status " +
            "ORDER BY c.createdOn DESC")
    List<Comment> findByStatus(@Param("status") CommentStatus status, Pageable pageable);

     @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.author " +
            "JOIN FETCH c.event " +
            "WHERE c.id = :id")
     Optional<Comment> findByIdWithDetails(@Param("id") Long id);
}