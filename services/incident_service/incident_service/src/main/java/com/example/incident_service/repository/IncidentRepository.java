
package com.example.incident_service.repository;
import com.example.incident_service.entity.Incident;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository
        extends JpaRepository<Incident, Long> {

    Optional<Incident> findByFingerprintAndStatusIn(
            String fingerprint,
            List<String> statuses
    );

    List<Incident> findByStatus(String status);

    List<Incident> findByTeamIgnoreCase(String team);

    List<Incident> findByStatusAndTeamIgnoreCase(
            String status,
            String team
    );

    Optional<Incident> findByIdAndTeamIgnoreCase(
            Long id,
            String team
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Incident i WHERE i.id = :id")
    Optional<Incident> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Incident i
       SET i.ownerUserId = :userId,
           i.ownerUsername = :username,
           i.status = 'IN_PROGRESS'
     WHERE i.id = :id
       AND i.status = 'OPEN'
       AND i.ownerUserId IS NULL
       AND UPPER(i.team) = UPPER(:team)
    """)
    int claimTeamIncident(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("username") String username,
            @Param("team") String team
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Incident i
       SET i.ownerUserId = :userId,
           i.ownerUsername = :username,
           i.status = 'IN_PROGRESS'
     WHERE i.id = :id
       AND i.status = 'OPEN'
       AND i.ownerUserId IS NULL
    """)
    int claimAnyIncident(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("username") String username
    );
    // Incidents opened since a given time
    List<Incident> findByOpenedAtGreaterThanEqual(Instant openedAt);

    // Incidents resolved since a given time
    List<Incident> findByResolvedAtGreaterThanEqual(Instant resolvedAt);

    // Team-specific incident creation history
    List<Incident> findByTeamIgnoreCaseAndOpenedAtGreaterThanEqual(
            String team,
            Instant openedAt
    );

    // Team-specific incident resolution history
    List<Incident> findByTeamIgnoreCaseAndResolvedAtGreaterThanEqual(
            String team,
            Instant resolvedAt
    );

    // Count incidents that are OPEN or IN_PROGRESS
    long countByStatusIn(List<String> statuses);

    // Count active incidents belonging to one team
    long countByTeamIgnoreCaseAndStatusIn(
            String team,
            List<String> statuses
    );
}
