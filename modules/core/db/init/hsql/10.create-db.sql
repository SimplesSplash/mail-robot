-- begin MAILREADER_TASK
create table MAILREADER_TASK (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SHORTDESC varchar(255),
    TESTING_PLAN varchar(255),
    PLANNING_TIME double precision,
    ACTUAL_TIME double precision,
    --
    primary key (ID)
)^
-- end MAILREADER_TASK
-- begin MAILREADER_TRACKER
create table MAILREADER_TRACKER (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    STEP_NAME varchar(255),
    SHORT_DESC varchar(255),
    DESCRIPTION longvarchar,
    PROJECT_ID varchar(36),
    TYPE_ varchar(255),
    TRACKER_PRIORITY_TYPE varchar(255),
    --
    primary key (ID)
)^
-- end MAILREADER_TRACKER
