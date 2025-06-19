CREATE
  TABLE
    item(
      id BIGINT PRIMARY KEY NOT NULL,
      version int8 NOT NULL,
      name VARCHAR(255) NOT NULL,
      price_in_cents int8 NOT NULL
    );
