-- begin MAILREADER_CONNECTION_DATA
create table MAILREADER_CONNECTION_DATA (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SERVER varchar(255),
    PORT integer,
    USERNAME varchar(255),
    PASSWORD varchar(255),
    PROTO varchar(255),
    CREDENTIALS text,
    REFRESH_TOKEN varchar(255),
    --
    primary key (ID)
)^
-- end MAILREADER_CONNECTION_DATA
