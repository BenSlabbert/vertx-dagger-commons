create
  table
  item (
  id bigint primary key not null,
  version int8 not null,
  name varchar(255) not null,
  price_in_cents int8 not null
);
