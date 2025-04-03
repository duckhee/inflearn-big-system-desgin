package kr.co.won.common.outboxmessagerelay.configuration;

import lombok.Getter;

import java.util.List;
import java.util.stream.LongStream;

@Getter
public class AssignedShard {

    // application에 할당된 Shard에 대한 키 값 리스트
    private List<Long> shards;


    public static AssignedShard of(String appId, List<String> appIds, long shardCount) {
        AssignedShard assignedShard = new AssignedShard();
        assignedShard.shards = assign(appId, appIds, shardCount);
        return assignedShard;
    }

    private static List<Long> assign(String appId, List<String> appIds, long shardCount) {
        int appIndex = findAppIndex(appId, appIds);
        if (appIndex == -1) {
            return List.of();
        }
        long start = appIndex * shardCount / appIds.size();
        long end = (appIndex + 1) * shardCount / appIds.size()-1;


        return LongStream.rangeClosed(start, end)
                .boxed()
                .toList();
    }

    // 실행된 어플리케이션에 대한 정렬된 형태로 가지고 있기 때문에 해당 어플리케이션 아이디가 몇 번째에 있는지를 찾아서 반환을 해준다.
    private static int findAppIndex(String appId, List<String> appIds) {
        for (int i = 0; i < appIds.size(); i++) {
            if (appIds.get(i).equals(appId)) {
                return i;
            }
        }
        return -1;
    }

}
