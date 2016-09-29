CREATE TABLE showcolumn1 (col1 int, `col 2` int);
CREATE TABLE showcolumn2 (price int, qty int) partitioned by (year int, month int);

-- only table name
SHOW COLUMNS IN showcolumn1;

-- qualified table name
SHOW COLUMNS IN default.showcolumn1;

-- table name and database name
SHOW COLUMNS IN showcolumn1 FROM default;

-- partitioned table
SHOW COLUMNS IN showcolumn2 IN default;

-- Non-existent table. Raise an error in this case
SHOW COLUMNS IN badtable FROM default;

-- database in table identifier and database name in different case
SHOW COLUMNS IN default.showcolumn1 from DEFAULT;

-- different database name in table identifier and database name.
-- Raise an error in this case.
SHOW COLUMNS IN default.showcolumn1 FROM baddb;

DROP TABLE showcolumn1;
DROP TABLE showColumn2;
