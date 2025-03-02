# database create
CREATE DATABASE IF NOT EXISTS `platform_comment`;
CREATE DATABASE IF NOT EXISTS `platform_article`;

# user add grant
GRANT SELECT, INSERT, UPDATE, DELETE ON platform_comment.* TO 'big_system'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON platform_article.* TO 'big_system'@'%';


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


create table platform_comment.tbl_comment
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
CREATE INDEX idx_article_id_parent_comment_id_comment_id ON platform_comment.tbl_comment (
	article_id ASC, parent_comment_id ASC, comment_id ASC
);

# 무한 댓글을 위한 테이블
CREATE TABLE tbl_infinity_comments(
	comment_id BIGINT NOT NULL PRIMARY KEY,
	content VARCHAR(3000) NOT NULL,
	article_id BIGINT NOT NULL,
	writer_id BIGINT NOT NULL,
	path VARCHAR(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
	deleted BOOL NOT NULL,
	created_at DATETIME NOT NULL
);

# 무한 댓글에서 사용을 할 인덱스
CREATE UNIQUE INDEX idx_article_id_path ON tbl_infinity_comments( article_id ASC, path ASC);