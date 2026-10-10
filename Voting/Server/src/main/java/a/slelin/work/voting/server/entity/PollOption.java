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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Вариант ответа. {@code position} задаёт порядок вариантов внутри голосования.
 */
@Getter
@Entity
@Table(name = "poll_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 100)
    private String text;

    PollOption(Poll poll, int position, String text) {
        this.poll = poll;
        this.position = position;
        this.text = text;
    }
}
