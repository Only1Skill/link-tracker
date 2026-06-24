package backend.academy.linktracker.scrapper.repository.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false, unique = true)
    private String url;

    @Column(name = "last_check_time", nullable = false)
    private OffsetDateTime lastCheckTime;

    @Column(name = "last_update_time", nullable = false)
    private OffsetDateTime lastUpdateTime;

    @OneToMany(mappedBy = "link")
    @Builder.Default
    private Set<SubscriptionEntity> subscriptions = new HashSet<>();
}
