# database create
CREATE DATABASE IF NOT EXISTS `platform_comment`;
CREATE DATABASE IF NOT EXISTS `platform_article`;
CREATE DATABASE IF NOT EXISTS `platform_article_like`;
CREATE DATABASE IF NOT EXISTS `platform_article_view`;

# user add grant -> if need to specific user create and match
GRANT SELECT, INSERT, UPDATE, DELETE ON platform_comment.* TO 'big_system'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON platform_article.* TO 'big_system'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON platform_article_like.* TO 'big_system'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON platform_article_view.* TO 'big_system'@'%';
#GRANT ALL PRIVILEGES ON platform_article_like.* TO 'big_system'@'%';

# 권한에 대한 적용
FLUSH PRIVILEGES;

# create database
CREATE TABLE platform_article.tbl_article
(
    article_id  BIGINT        NOT NULL PRIMARY KEY COMMENT "This is article ID",
    title       VARCHAR(100)  NOT NULL COMMENT "article title",
    content     VARCHAR(3000) NOT NULL COMMENT "article content",
    board_id    BIGINT        NOT NULL COMMENT "Board ID (Shard Key)",
    writer_id   BIGINT        NOT NULL COMMENT "writer ID",
    created_at  DATETIME      NOT NULL DEFAULT NOW() COMMENT "article create time",
    modified_at DATETIME      NOT NULL COMMENT "article modified time"
);

/*날짜에 대한 Index 를 생성하는 Query */
# CREATE INDEX idx_board_id_article_id ON platform_article.tbl_article (board_id ASC, article_id DESC);
CREATE INDEX idx_board_id_article_id ON platform_article.tbl_article (board_id ASC, article_id DESC);

# 게시글 수에 대한 테이블 생성
CREATE TABLE platform_article.tbl_board_article_count(
    board_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is article count Primary key -> using shard key",
    article_count BIGINT NOT NULL COMMENT "article count number"
);


# 분산 트랜잭션을 위한 테이블 -> Transactional Outbox
CREATE TABLE platform_article.tbl_outbox(
    outbox_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is outbox PK",
    shard_key BIGINT NOT NULL COMMENT "service event shard key",
    event_type VARCHAR(100) NOT NULL COMMENT "event type",
    payload VARCHAR(5000) NOT NULL COMMENT "event payload",
    created_at DATETIME NOT NULL COMMENT "event created time"
);

# 분산 트랜잭션에 대한 index -> 생성 10초 이후 조건 조회를 위한 인덱스
CREATE INDEX idx_shard_key_created_at ON platform_article.tbl_outbox(shard_key ASC, created_at ASC);



CREATE TABLE platform_comment.tbl_comment
(
    comment_id        BIGINT        NOT NULL PRIMARY KEY COMMENT "This is comment ID",
    content           VARCHAR(3000) NOT NULL COMMENT "This is comment content",
    article_id        BIGINT        NOT NULL COMMENT "article ID(Shard Key)",
#     parent_comment_id BIGINT COMMENT "Parent comment id",
    parent_comment_id BIGINT        NOT NULL COMMENT "Parent comment id",
    writer_id         BIGINT        NOT NULL COMMENT "comment write user id",
    deleted           BOOL          NOT NULL DEFAULT FALSE COMMENT "comment delete or not flag",
    created_at        DATETIME      NOT NULL COMMENT "comment write time"
);

/** 댓글에 대한 목록 조회 시 사용할 Index를 생성을 하는 Query */
CREATE INDEX idx_article_id_parent_comment_id_comment_id ON `platform_comment`.tbl_comment( article_id ASC, parent_comment_id ASC, comment_id ASC);

# 무한 댓글을 위한 테이블
CREATE TABLE platform_comment.tbl_infinity_comments(
	comment_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is infinity comment ID",
	content VARCHAR(3000) NOT NULL COMMENT "This is comment content",
	article_id BIGINT NOT NULL COMMENT "article ID(Shard Key)",
	writer_id BIGINT NOT NULL COMMENT "writer ID",
	path VARCHAR(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT "depth path using Tree path",
	deleted BOOL NOT NULL COMMENT "This is checking delete or not flag",
	created_at DATETIME NOT NULL COMMENT "comment write time"
);

# 댓글 수에 대한 테이블 생성
CREATE TABLE platform_comment.tbl_article_comment_count(
    article_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is article comment count Primary Key -> using shard key",
    comment_count BIGINT NOT NULL COMMENT "This is article comment number"
);

# 무한 댓글에서 사용을 할 인덱스
CREATE UNIQUE INDEX  idx_article_id_path ON `platform_comment`.tbl_infinity_comments( article_id ASC, path ASC);


# 분산 트랜잭션을 위한 테이블 -> Transactional Outbox
CREATE TABLE platform_comment.tbl_outbox(
    outbox_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is outbox PK",
    shard_key BIGINT NOT NULL COMMENT "service event shard key",
    event_type VARCHAR(100) NOT NULL COMMENT "event type",
    payload VARCHAR(5000) NOT NULL COMMENT "event payload",
    created_at DATETIME NOT NULL COMMENT "event created time"
);

# 분산 트랜잭션에 대한 index -> 생성 10초 이후 조건 조회를 위한 인덱스
CREATE INDEX idx_shard_key_created_at ON platform_comment.tbl_outbox(shard_key ASC, created_at ASC);



# 좋아요를 위한 테이블
CREATE TABLE platform_article_like.tbl_article_like
(
    article_like_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is article like PK",
    article_id BIGINT NOT NULL COMMENT "article ID this is using shard key",
    user_id BIGINT NOT NULL COMMENT "article like user",
    created_at DATETIME NOT NULL COMMENT "user push article like date time"
);

# 좋아요에 대한 게시글에 대한 제약 조건인 게시글에는 한 사용자가 1개만 좋아요를 할 수 있다는 것을 표현하기 위한 UNIQUE INDEX
CREATE UNIQUE INDEX idx_article_id_user_id ON `platform_article_like`.tbl_article_like(article_id ASC, user_id ASC );

# 좋아요 수에 대한 테이블
CREATE TABLE platform_article_like.tbl_article_like_count
(
    article_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is article like count PK",
    like_count BIGINT NOT NULL COMMENT "This is like number",
    version BIGINT NOT NULL COMMENT "optimistic lock version column"
);


# 분산 트랜잭션을 위한 테이블 -> Transactional Outbox
CREATE TABLE platform_article_like.tbl_outbox(
    outbox_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is outbox PK",
    shard_key BIGINT NOT NULL COMMENT "service event shard key",
    event_type VARCHAR(100) NOT NULL COMMENT "event type",
    payload VARCHAR(5000) NOT NULL COMMENT "event payload",
    created_at DATETIME NOT NULL COMMENT "event created time"
);

# 분산 트랜잭션에 대한 index -> 생성 10초 이후 조건 조회를 위한 인덱스
CREATE INDEX idx_shard_key_created_at ON platform_article_like.tbl_outbox(shard_key ASC, created_at ASC);



# 조회 수에 대한 백업 테이블
CREATE TABLE platform_article_view.tbl_article_view_count(
    article_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is view count PK and Shard Key",
    view_count BIGINT NOT NULL COMMENT "view count back up"
);

# 분산 트랜잭션을 위한 테이블 -> Transactional Outbox
CREATE TABLE platform_article_view.tbl_outbox(
    outbox_id BIGINT NOT NULL PRIMARY KEY COMMENT "This is outbox PK",
    shard_key BIGINT NOT NULL COMMENT "service event shard key",
    event_type VARCHAR(100) NOT NULL COMMENT "event type",
    payload VARCHAR(5000) NOT NULL COMMENT "event payload",
    created_at DATETIME NOT NULL COMMENT "event created time"
);

# 분산 트랜잭션에 대한 index -> 생성 10초 이후 조건 조회를 위한 인덱스
CREATE INDEX idx_shard_key_created_at ON platform_article_view.tbl_outbox(shard_key ASC, created_at ASC);

