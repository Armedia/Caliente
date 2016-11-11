drop table caliente_status cascade constraints
create table caliente_status (
	i_chronicle_id	varchar(16) not null,
	archetype		number(1) not null,
	object_path		varchar(740) not null,
	object_name		varchar(255) not null,
	modified		varchar(24) not null,
	path_depth		number(4) not null,
	constraint		caliente_status_pk primary key (i_chronicle_id)
);
create unique index idx_caliente_status_path on caliente_status (object_path, object_name);
create index idx_caliente_stauts_mod on caliente_status (modified, object_path, object_name);

drop table caliente_version cascade constraints;
create table caliente_version (
	r_object_id		varchar(16) not null,
	i_chronicle_id	varchar(16) not null,
	version_label	varchar(1024) not null,
	constraint		caliente_version_pk primary key (r_object_id),
	constraint		caliente_version_fk foreign key (i_chronicle_id) references caliente_status (i_chronicle_id) on delete cascade
);