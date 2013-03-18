create table book_source (
  url varchar (1024),
  url_hash char (32),
  bookid char(32),
  format varchar(32),
  primary key (url_hash)
);
create index book_source_bookid on book_source (bookid);

create table book (
  bookid char(32),
  format varchar(32),
  file_name varchar (1024),
  primary key (bookid, format)
);

create table batch (
  batchid char(32),
  format varchar(32),
  file_name varchar (1024),
  primary key (batchid, format)
);