package a.slelin.work.voting.server.entity;

import a.slelin.work.voting.common.dto.PollStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Голосование: вопрос, варианты ответа и статус. Голоса хранятся отдельно — в {@link Vote}.
 */
@Getter
@Entity
@Table(name = "polls")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String question;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PollStatus status = PollStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OrderBy("position")
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOption> options = new ArrayList<>();

    /**
     * Счётчики вычисляются базой при загрузке голосования — так список строится одним запросом,
     * без загрузки вариантов и голосов каждого голосования.
     */
    @Formula("(select count(*) from votes v where v.poll_id = id)")
    private long totalVotes;

    @Formula("(select count(*) from poll_options o where o.poll_id = id)")
    private int optionsCount;

    public Poll(String question, String description, List<String> options) {
        this.question = question;
        this.description = description;
        this.createdAt = Instant.now();
        for (int i = 0; i < options.size(); i++) {
            this.options.add(new PollOption(this, i + 1, options.get(i)));
        }
    }

    public boolean isOpen() {
        return status == PollStatus.OPEN;
    }

    public void close() {
        if (isOpen()) {
            status = PollStatus.CLOSED;
            closedAt = Instant.now();
        }
    }
}
