package a.slelin.work.voting.common.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PollStatus {

    OPEN("Открыто"),
    CLOSED("Завершено");

    private final String title;
}
