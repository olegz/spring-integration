-- Autogenerated: do not edit this file

CREATE TABLE INT_MESSAGE  (
	MESSAGE_ID CHAR(36) NOT NULL PRIMARY KEY,
	REGION VARCHAR(100),
	CREATED_DATE DATETIME NOT NULL,
	MESSAGE_BYTES BLOB
) ENGINE=InnoDB;

CREATE INDEX INT_MESSAGE_IX1 ON INT_MESSAGE (CREATED_DATE);

CREATE TABLE INT_GROUP_TO_MESSAGE  (
	GROUP_KEY CHAR(36) NOT NULL,
	MESSAGE_ID CHAR(36) NOT NULL,
	constraint MESSAGE_GROUP_PK primary key (GROUP_KEY, MESSAGE_ID)
) ENGINE=InnoDB;

CREATE TABLE INT_MESSAGE_GROUP  (
    GROUP_KEY CHAR(36) NOT NULL PRIMARY KEY,
	REGION VARCHAR(100),
	MARKED BIGINT,
	COMPLETE BIGINT,
	LAST_RELEASED_SEQUENCE BIGINT,
	CREATED_DATE DATETIME NOT NULL,
	UPDATED_DATE DATETIME DEFAULT NULL
) ENGINE=InnoDB;