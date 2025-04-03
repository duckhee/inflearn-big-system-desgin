package kr.co.won.common.outboxmessagerelay.configuration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AssignedShardTest {

    @DisplayName(value = "01. factory method 생성 테스트")
    @Test
    void ofCreateFactoryTests() {
        // given
        Long shardCount = 64l;
        List<String> appIdes = List.of("appId1", "appId2", "appId3");

        // when
        AssignedShard app1 = AssignedShard.of(appIdes.get(0), appIdes, shardCount);
        AssignedShard app2 = AssignedShard.of(appIdes.get(1), appIdes, shardCount);
        AssignedShard app3 = AssignedShard.of(appIdes.get(2), appIdes, shardCount);
        AssignedShard app4 = AssignedShard.of("invalid", appIdes, shardCount);

        // then
        List<Long> result = Stream.of(app1.getShards(), app2.getShards(), app3.getShards(), app4.getShards())
                .flatMap(List::stream)
                .toList();

        Assertions.assertThat(result).hasSize(shardCount.intValue());

        for(int i = 0; i < shardCount.intValue();i++){
            Assertions.assertThat(result.get(i)).isEqualTo(i);
        }

        Assertions.assertThat(app4.getShards()).isEmpty();


    }
}