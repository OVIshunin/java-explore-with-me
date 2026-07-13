package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long userId);

    List<ParticipationRequest> findByEventId(Long eventId);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    long countConfirmedRequests(@Param("eventId") Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long userId);

    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    @Query("SELECT r FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.id IN :requestIds")
    List<ParticipationRequest> findByEventIdAndRequestIds(@Param("eventId") Long eventId,
                                                          @Param("requestIds") List<Long> requestIds);
}