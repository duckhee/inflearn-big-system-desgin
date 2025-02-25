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