package backend.academy.linktracker.scrapper.repository.jpa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "link_chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionEntity {

    @EmbeddedId
    private SubscriptionId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("linkId")
    @JoinColumn(name = "link_id", nullable = false)
    private LinkEntity link;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SubscriptionTagEntity> subscriptionTags = new HashSet<>();

    public void addTag(TagEntity tag) {
        boolean alreadyExists = subscriptionTags.stream()
                .anyMatch(st -> Objects.equals(st.getTag().getId(), tag.getId()));

        if (alreadyExists) {
            return;
        }

        SubscriptionTagEntity subscriptionTag = SubscriptionTagEntity.builder()
                .id(new SubscriptionTagId(chat.getId(), link.getId(), tag.getId()))
                .subscription(this)
                .tag(tag)
                .build();

        subscriptionTags.add(subscriptionTag);
    }
}
