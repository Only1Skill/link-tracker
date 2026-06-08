package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.repository.jpa.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.jpa.entity.SubscriptionId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionEntity, SubscriptionId> {

    @EntityGraph(attributePaths = {"chat", "link", "subscriptionTags", "subscriptionTags.tag"})
    List<SubscriptionEntity> findByChat_Id(Long chatId);

    @EntityGraph(attributePaths = {"chat", "link", "subscriptionTags", "subscriptionTags.tag"})
    Optional<SubscriptionEntity> findByChat_IdAndLink_Url(Long chatId, String url);

    @EntityGraph(attributePaths = {"chat", "link", "subscriptionTags", "subscriptionTags.tag"})
    List<SubscriptionEntity> findByLink_Url(String url);

    @EntityGraph(attributePaths = {"chat", "link", "subscriptionTags", "subscriptionTags.tag"})
    @Query("""
        select distinct s
        from SubscriptionEntity s
        join s.subscriptionTags st
        join st.tag t
        where s.chat.id = :chatId and t.name = :tag
        """)
    List<SubscriptionEntity> findByChatIdAndTag(@Param("chatId") Long chatId, @Param("tag") String tag);

    @EntityGraph(attributePaths = {"chat", "link", "subscriptionTags", "subscriptionTags.tag"})
    @Query("select distinct s from SubscriptionEntity s")
    List<SubscriptionEntity> findAllWithGraph();

    boolean existsByLink_Id(Long linkId);

    @Query("""
        select s.chat.id
        from SubscriptionEntity s
        where s.link.id = :linkId
        """)
    List<Long> findChatIdsByLinkId(@Param("linkId") Long linkId);
}
