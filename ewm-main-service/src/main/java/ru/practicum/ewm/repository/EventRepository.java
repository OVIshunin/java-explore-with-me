package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findEventsByAdmin(@Param("users") List<Long> users,
                                  @Param("states") List<EventState> states,
                                  @Param("categories") List<Long> categories,
                                  @Param("rangeStart") LocalDateTime rangeStart,
                                  @Param("rangeEnd") LocalDateTime rangeEnd,
                                  Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (COALESCE(:text, '') = '' OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (COALESCE(:categories, NULL) IS NULL OR e.category.id IN :categories) " +
            "AND (COALESCE(:paid, NULL) IS NULL OR e.paid = :paid) " +
            "AND (COALESCE(:rangeStart, CURRENT_TIMESTAMP) IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (COALESCE(:rangeEnd, CURRENT_TIMESTAMP) IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findEventsPublic(@Param("text") String text,
                                 @Param("categories") List<Long> categories,
                                 @Param("paid") Boolean paid,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 Pageable pageable);

    @Query(value = "SELECT * FROM events e " +
            "WHERE (CAST(:users AS TEXT) IS NULL OR e.initiator_id IN (:users)) " +
            "AND (CAST(:states AS TEXT) IS NULL OR e.state IN (:states)) " +
            "AND (CAST(:categories AS TEXT) IS NULL OR e.category_id IN (:categories)) " +
            "AND e.event_date >= COALESCE(CAST(:rangeStart AS TIMESTAMP), '2020-01-01') " +
            "AND e.event_date <= COALESCE(CAST(:rangeEnd AS TIMESTAMP), '2099-12-31') " +
            "ORDER BY e.event_date " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Event> findEventsByAdminNative(@Param("users") List<Long> users,
                                        @Param("states") List<String> states,
                                        @Param("categories") List<Long> categories,
                                        @Param("rangeStart") LocalDateTime rangeStart,
                                        @Param("rangeEnd") LocalDateTime rangeEnd,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (CAST(:text AS TEXT) IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', CAST(:text AS TEXT), '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:text AS TEXT), '%'))) " +
            "AND (CAST(:categories AS TEXT) IS NULL OR e.category_id IN (:categories)) " +
            "AND (CAST(:paid AS TEXT) IS NULL OR e.paid = :paid) " +
            "AND e.event_date >= COALESCE(CAST(:rangeStart AS TIMESTAMP), CURRENT_TIMESTAMP) " +
            "AND e.event_date <= COALESCE(CAST(:rangeEnd AS TIMESTAMP), '2099-12-31') " +
            "ORDER BY e.event_date " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Event> findEventsPublicNative(@Param("text") String text,
                                       @Param("categories") List<Long> categories,
                                       @Param("paid") Boolean paid,
                                       @Param("rangeStart") LocalDateTime rangeStart,
                                       @Param("rangeEnd") LocalDateTime rangeEnd,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);


    Optional<Event> findByIdAndState(Long eventId, EventState state);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByInitiatorId(Long userId);
}