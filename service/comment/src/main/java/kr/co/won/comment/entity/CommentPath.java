package kr.co.won.comment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentPath {

    /**
     * path에서 사용을 할 문자열에 대한 정의
     */
    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * 하나의 Depth 당 사용할 문자열의 갯수
     */
    private static final int DEPTH_CHUNK_SIZE = 5;

    /**
     * 최대 표현이 가능한 Depth에 대한 정의
     */
    private static final int MAX_DEPTH = 5;

    /**
     * Depth에 대한 Path에 대한 표현 중 가장 작은 값
     */
    private static final String MIN_CHUNK = String.valueOf(CHARSET.charAt(0)).repeat(DEPTH_CHUNK_SIZE);

    /**
     * Depth에 대한 Path에 대한 표현 중 가장 큰 값
     */
    private static final String MAX_CHUNK = String.valueOf(CHARSET.charAt(CHARSET.length() - 1)).repeat(DEPTH_CHUNK_SIZE);


    @Column(name = "path")
    private String path;

    public static CommentPath create(String path) {
        /** 표현할 수 있는 댓글의 path 넘어 갔는지 확인 */
        if (isDepthOverflowed(path)) {
            throw new IllegalArgumentException("depth overflow");
        }
        CommentPath commentPath = new CommentPath();
        commentPath.path = path;
        return commentPath;
    }

    private static boolean isDepthOverflowed(String path) {
        /** 현재 호출한 Depth 가 최대로 표현을 할 수 있는 Depth 를 넘어갈 경우 Overflow가 발생한다. */
        return callDepth(path) > MAX_DEPTH;
    }

    /**
     * 호출한 Depth가 몇 Depth 인지 확인 하는 함수
     */
    private static int callDepth(String path) {
        /** 하나의 Depth를 표현을 하는 길이가 5이기 때문에 5로 나누어 준다. */
        return path.length() / 5;
    }

    /**
     * 현재 Depth에 대해서 구하는 함수
     */
    public int getDepth() {
        return path.length() / 5;
    }

    /**
     * 현재 댓글이 Root 댓글인지를 확인하는 함수
     */
    public boolean isRootComment() {
        return getDepth() == 1;
    }

    /**
     * 현재 Path의 부모 Path를 반환하는 함수
     */
    public String getParentPath() {
        /** 처음부터 끝의 다섯자리를 뺀 값을 구하면 부모 path가 된다. */
        return path.substring(0, path.length() - DEPTH_CHUNK_SIZE);
    }

    /**
     * 현재 Path의 하위 댓글의 Path를 생성하는 함수
     */
    public CommentPath getChildCommentPath(String descendantsTopPath) {
        /** 하위 댓글이 처음 생성이 되는 경우 */
        if (descendantsTopPath == null) {
            return CommentPath.create(this.path + MIN_CHUNK);
        }
        String childrenTopPath = findChildrenTopPath(descendantsTopPath);
        return CommentPath.create(increase(childrenTopPath));
    }


    private String findChildrenTopPath(String descendantsTopPath) {

        String childCommentPath = descendantsTopPath.substring(0, (getDepth() + 1) * DEPTH_CHUNK_SIZE);

        return childCommentPath;
    }


    private String increase(String childrenCommentPath) {
        String lastChunk = childrenCommentPath.substring(childrenCommentPath.length() - DEPTH_CHUNK_SIZE);
        if (isChunkOverflowed(lastChunk)) {
            throw new IllegalArgumentException("chunk overflow");
        }

        int charsetLength = CHARSET.length();

        int value = 0;
        /** 문자열에 대한 값을 10진수로 변환을 해주는 과정 */
        for (char ch : lastChunk.toCharArray()) {
            value = value * charsetLength + CHARSET.indexOf(ch);
        }

        /** 다음 값을 구하기 */
        value = value + 1;

        String resultPath = "";
        /** path를 구하는 과정 */
        for (int i = 0; i < DEPTH_CHUNK_SIZE; i++) {
            resultPath = CHARSET.charAt(value % charsetLength) + resultPath;
            value /= charsetLength;
        }

        return childrenCommentPath.substring(0, childrenCommentPath.length() - DEPTH_CHUNK_SIZE) + resultPath;
    }

    private boolean isChunkOverflowed(String lastChunk) {
        return MAX_CHUNK.equals(lastChunk);
    }
}
