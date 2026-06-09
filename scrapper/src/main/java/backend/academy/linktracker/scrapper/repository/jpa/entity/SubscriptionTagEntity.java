package backend.academy.linktracker.scrapper.repository.jpa.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "link_chat_tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionTagEntity {

    @EmbeddedId
    private SubscriptionTagId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "chat_id", referencedColumnName = "chat_id", insertable = false, updatable = false),
        @JoinColumn(name = "link_id", referencedColumnName = "link_id", insertable = false, updatable = false)
    })
    private SubscriptionEntity subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", referencedColumnName = "id", insertable = false, updatable = false)
    private TagEntity tag;
}
