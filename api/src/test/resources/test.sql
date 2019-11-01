delete from users;
delete from spots;
delete from reservations;
drop table if exists users;
drop table if exists spots;
drop table if exists reservations;

create table users (id integer not null auto_increment primary key, phone_number text, validated bool, validation_code text, password_hash text, password_salt text);
create table spots (id integer not null auto_increment primary key, spot_name text not null, lat decimal (10,8) not null, lon decimal (11, 8) not null, state text not null, rate int not null, reserved_till datetime);
create table reservations (id integer not null auto_increment primary key, state text not null, spotid integer not null references spots(id) on delete cascade, userid integer not null references users(id) on delete cascade, start datetime not null, end datetime not null, duration integer not null, cost integer not null);

insert into users (phone_number, validated) values ('1234567890', true);
insert into users (phone_number, validated) values ('0987654321', true);

/* In tests we will be querying from 18.923715, 72.8311921 i.e. Hotel Taj Mahal Mumbai. */

/* unreserved spot within query radius - gateway of india */
insert into spots (spot_name, lat, lon, state, rate) values ('Gateway of India', 18.9219841, 72.8324656, 'FREE', 120);

/* unreserved spot out of query radius - India gate, Delhi */
insert into spots (spot_name, lat, lon, state, rate) values ('India Gate', 28.6090482, 77.2241501, 'FREE', 200);

/* reserved spot within query radius */
insert into spots (spot_name, lat, lon, state, rate) values ('Taj Hotel', 18.923715, 72.8311921, 'RESERVED', 120);

/* Active reservation */
insert into reservations (spotid, userid, state, start, end, duration, cost) values (3, 1, 'ACTIVE', curtime(), curtime(), 1, 23);

/* Reservation that has ended */
insert into reservations (spotid, userid, state, start, end, duration, cost) values (2, 1, 'ENDED', curtime(), curtime(), 1, 24);

/* Reservation that was cancelled */
insert into reservations (spotid, userid, state, start, end, duration, cost) values (1, 1, 'CANCELLED', curtime(), curtime(), 1, 10);

/* Reservation from other user */
insert into reservations (spotid, userid, state, start, end, duration, cost) values (1, 2, 'ACTIVE', curtime(), curtime(), 1, 10);