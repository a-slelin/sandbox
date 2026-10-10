package a.slelin.work.voting.server.repository;

import a.slelin.work.voting.server.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    /**
     * Количество голосов за вариант (проекция результата группировки).
     */
    interface OptionVotes {

        Long getOptionId();

        Long getVotes();
    }

    @Query("""
            select v.option.id as optionId, count(v) as votes
            from Vote v
            where v.poll.id = :pollId
            group by v.option.id""")
    List<OptionVotes> countByOption(@Param("pollId") UUID pollId);

    @Query("select v.option.id from Vote v where v.poll.id = :pollId and v.voter = :voter")
    Optional<Long> findOptionId(@Param("pollId") UUID pollId, @Param("voter") String voter);

    boolean existsByPollIdAndVoter(UUID pollId, String voter);

    @Modifying
    @Query("delete from Vote v where v.poll.id = :pollId")
    void deleteByPollId(@Param("pollId") UUID pollId);
}
