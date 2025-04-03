package kr.co.won.common.outboxmessagerelay.repository;

import kr.co.won.common.outboxmessagerelay.entity.OutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {

    /**
     * 미발송된 이벤트에 대해서 주기적으로 가져오기 위한 SQL
     *
     * @param shardKey
     * @param from
     * @param pageable
     * @return
     */
    List<OutboxEntity> findAllByShardKeyAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            Long shardKey,
            LocalDateTime from,
            Pageable pageable
    );
}
