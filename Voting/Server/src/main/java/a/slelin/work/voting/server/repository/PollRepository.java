package a.slelin.work.voting.server.repository;

import a.slelin.work.voting.server.entity.Poll;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PollRepository extends JpaRepository<Poll, UUID> {

    /**
     * Загружает голосование с блокировкой строки до конца транзакции:
     * голоса и завершение одного голосования выполняются строго по очереди.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Poll p where p.id = :id")
    Optional<Poll> findByIdForUpdate(@Param("id") UUID id);
}
