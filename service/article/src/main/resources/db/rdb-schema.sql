CREATE TABLE tbl_article
(
    article_id  BIGINT        NOT NULL PRIMARY KEY COMMENT "This is article ID",
    title       VARCHAR(100)  NOT NULL COMMENT "article title",
    content     VARCHAR(3000) NOT NULL COMMENT "article content",
    board_id    BIGINT        NOT NULL COMMENT "Board ID",
    writer_id   BIGINT        NOT NULL COMMENT "writer ID",
    created_at  DATETIME      NOT NULL DEFAULT NOW() COMMENT "article create time",
    modified_at DATETIME      NOT NULL COMMENT "article modified time"
);