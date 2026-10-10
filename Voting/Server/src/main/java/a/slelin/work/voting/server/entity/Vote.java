package a.slelin.work.voting.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;

/**
 * Голос участника. Уникальность пары (голосование, участник) гарантирует база:
 * даже при одновременных запросах второй голос не сохранится.
 */
@Getter
@Entity
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(
        name = "uk_votes_poll_voter", columnNames = {"poll_id", "voter"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    /**
     * Имя участника в нормализованном виде (без пробелов по краям, в нижнем регистре).
     */
    @Column(nullable = false, length = 50)
    private String voter;

    @Column(name = "voted_at", nullable = false)
    private Instant votedAt;

    public Vote(Poll poll, PollOption option, String voter) {
        this.poll = poll;
        this.option = option;
        this.voter = normalize(voter);
        this.votedAt = Instant.now();
    }

    public static String normalize(String voter) {
        return voter.trim().toLowerCase(Locale.ROOT);
    }
}
