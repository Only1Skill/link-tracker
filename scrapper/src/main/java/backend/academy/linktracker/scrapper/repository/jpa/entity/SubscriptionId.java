package backend.academy.linktracker.scrapper.repository.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SubscriptionId implements Serializable {

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "link_id")
    private Long linkId;
}
