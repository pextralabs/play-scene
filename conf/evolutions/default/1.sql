# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table entity (
  id                            bigint auto_increment not null,
  name                          varchar(255),
  type                          varchar(255),
  constraint pk_entity primary key (id)
);

create table sensor (
  id                            bigint auto_increment not null,
  key                           varchar(255),
  name                          varchar(255),
  description                   varchar(255),
  tag                           varchar(255),
  constraint pk_sensor primary key (id)
);

create table entity_sensor (
  id                            bigint auto_increment not null,
  bearer_id                     bigint not null,
  label                         varchar(255),
  value                         double,
  constraint pk_entity_sensor primary key (id)
);

create table stream (
  id                            bigint auto_increment not null,
  key                           varchar(255),
  name                          varchar(255),
  tag                           varchar(255),
  sensor_id                     bigint not null,
  constraint pk_stream primary key (id)
);

create table stream_data (
  id                            bigint auto_increment not null,
  stream_id                     bigint not null,
  server_timestamp              bigint not null,
  sensor_timestamp              bigint not null,
  raw                           varchar(255),
  constraint pk_stream_data primary key (id)
);

create table tuple (
  id                            bigint auto_increment not null,
  data_id                       bigint not null,
  key                           varchar(255),
  value                         varchar(255),
  constraint pk_tuple primary key (id)
);

alter table entity_sensor add constraint fk_entity_sensor_bearer_id foreign key (bearer_id) references entity (id) on delete restrict on update restrict;
create index ix_entity_sensor_bearer_id on entity_sensor (bearer_id);

alter table stream add constraint fk_stream_sensor_id foreign key (sensor_id) references sensor (id) on delete restrict on update restrict;
create index ix_stream_sensor_id on stream (sensor_id);

alter table stream_data add constraint fk_stream_data_stream_id foreign key (stream_id) references stream (id) on delete restrict on update restrict;
create index ix_stream_data_stream_id on stream_data (stream_id);

alter table tuple add constraint fk_tuple_stream_data_id foreign key (data_id) references stream_data (id) on delete restrict on update restrict;
create index ix_tuple_stream_data_id on tuple (data_id);


# --- !Downs

alter table entity_sensor drop constraint if exists fk_entity_sensor_bearer_id;
drop index if exists ix_entity_sensor_bearer_id;

alter table stream drop constraint if exists fk_stream_sensor_id;
drop index if exists ix_stream_sensor_id;

alter table stream_data drop constraint if exists fk_stream_data_stream_id;
drop index if exists ix_stream_data_stream_id;

alter table tuple drop constraint if exists fk_tuple_stream_data_id;
drop index if exists ix_tuple_stream_data_id;

drop table if exists entity;

drop table if exists sensor;

drop table if exists entity_sensor;

drop table if exists stream;

drop table if exists stream_data;

drop table if exists tuple;

