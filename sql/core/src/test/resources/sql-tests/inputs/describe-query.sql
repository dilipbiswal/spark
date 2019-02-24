-- Test tables
CREATE table  desc_temp1 (key int COMMENT 'column_comment', val string) USING PARQUET;
CREATE table  desc_temp2 (key int, val string) USING PARQUET;
CREATE table  desc_temp3 (key int, val string) USING PARQUET;

-- Simple Describe query
DESC SELECT key, key + 1 as plusone FROM desc_temp1;
DESC QUERY SELECT * FROM desc_temp2;
DESC SELECT key, COUNT(*) as count FROM desc_temp1 group by key;
DESC SELECT 10.00D as col1;
DESC QUERY SELECT key FROM desc_temp1 UNION ALL select CAST(1 AS DOUBLE);
DESC WITH s AS (SELECT 'hello' as col1) SELECT * FROM s;
DESCRIBE QUERY WITH s AS (SELECT * from desc_temp1) SELECT * FROM s;
DESC QUERY VALUES(1.00D, 'hello') as tab1(col1, col2);


-- Error cases.
DESCRIBE INSERT INTO desc_temp1 values (1, 'val1');
DESCRIBE INSERT INTO desc_temp1 SELECT * FROM desc_temp2;
DESCRIBE
   FROM desc_temp1 a
     insert into desc_temp2 select *
     insert into desc_temp3 select *;

-- cleanup
DROP TABLE desc_temp1;
DROP TABLE desc_temp2;
DROP TABLE desc_temp3;