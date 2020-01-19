---
layout: global
title: FROM Clause
displayTitle: FROM Clause
license: |
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
 
     http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
---
The <code>FROM</code> clause provides the source of input for the query. The
other operators operate on the resultset produced by the <code>FROM</code>
clause.

### Syntax
{% highlight sql %}
FROM { { { relation_primary [ join_relation join_relation ...] } [ , ... ] } }
     { LATERAL VIEW [OUTER] table_function table_alias [ [ AS ]  col_alias [, ...] ] }
     { PIVOT ( aggregate_expressions ) FOR pivot_column IN ( pivot_values ) ) }
{% endhighlight %}

### Parameters
<dl>
  <dt><code><em>relation_primary</em></code></dt>
  <dd>
    Specifies a primary relation which can be one of 
    <br><br>
    <b>Syntax:</b>
    {% highlight sql %}
    table_identifier [ TABLESAMPLE [ ( sample_method ) ] ] [ [AS] table_alias ]
    | ( select_query ) [ TABLESAMPLE [ ( sample_method ) ] ] [ [AS] table_alias ]  
    | VALUES { expression [ , ... ] } [ [AS] table_alias ]
    | function_name( expression [, expression ] ) [ [AS] table_alias ]
    {% endhighlight %}
  </dd>
  <dt><code><em>join_relation</em></code></dt>
  <dd>
  </dd>
  <dt><code><em>LATERAL VIEW</em></code></dt>
  <dd>
    A lateral view is used to produces a result set based on a table function. The input 
    to table function may refer to columns from the tables that appear before the lateral view.
    A table function generates zero or more output rows based on its input arguments/expressions.
    The rows produced by a lateral view is joined with the base table to produce the output rows.
  </dd>
  <dt><code><em>OUTER</em></code></dt>
  <dd>
    If this optional keyword is specified, the <code>LATERAL VIEW</code> clause returns a row
    with NULL values when the underlying table generating function does not return any rows.
    In the absence of this keyword, the query will return empty results when the table generating
    function return empty result even though there are qualifying rows in the base table. 
  </dd>
  <dt><code><em>table_function</em></code></dt>
  <dd>
    Specifies a table function which generates zero or more output rows based on its input 
    arguments/expressions.  Spark has builtin table generating functions such as <code>explode</code>,
    <code>posexplode</code> and <code>stack</code> etc. 
  </dd>
  <dt><code><em>table_alias</em></code></dt>
  <dd>
    Assigns a temporary name to a relaton or lateral view. The alias may be used in the rest
    of sections of the query. In the context of `LATERAL VIEW` clause, a table alias is mandatory
    and must be specified.  
  </dd> 
  <dt><code><em>col_alias</em></code></dt>
  <dd>
    Assigns a temporary name to the columns produced by a relation or lateral view. The column aliases
    may be used in the rest of sections of the query. In the context of `LATERAL VIEW` clause, it is
    not mandatory to specify the column aliases.
  </dd> 
  <dt><code><em>PIVOT</em></code></dt>
  <dt><code><em>aggregate_expressions</em></code></dt>
  <dt><code><em>pivot_column</em></code></dt>
  <dt><code><em>pivot_values</em></code></dt>
</dl>

### Examples
{% highlight sql %}
CREATE TABLE person (id INT, name STRING, age INT);
INSERT INTO person VALUES (100, 'John', 30),
                          (200, 'Mary', NULL),
                          (300, 'Mike', 80),
                          (400, 'Dan',  50);

-- Comparison operator in `WHERE` clause.
SELECT * FROM person WHERE id > 200 ORDER BY id;
  +---+----+---+
  |id |name|age|
  +---+----+---+
  |300|Mike|80 |
  |400|Dan |50 |
  +---+----+---+

-- Comparison and logical operators in `WHERE` clause.
SELECT * FROM person WHERE id = 200 OR id = 300 ORDER BY id;
  +---+----+----+
  |id |name|age |
  +---+----+----+
  |200|Mary|null|
  |300|Mike|80  |
  +---+----+----+

-- ISNULL expression in `WHERE` clause.
SELECT * FROM person WHERE id > 300 OR age IS NULL ORDER BY id;
  +---+----+----+
  |id |name|age |
  +---+----+----+
  |200|Mary|null|
  |400|Dan |50  |
  +---+----+----+

-- Function expression in `WHERE` clause.
SELECT * FROM person WHERE length(name) > 3 ORDER BY id;
  +---+----+----+
  |id |name|age |
  +---+----+----+
  |100|John|30  |
  |200|Mary|null|
  |300|Mike|80  |
  +---+----+----+

-- `BETWEEN` expression in `WHERE` clause.
SELECT * FROM person WHERE id BETWEEN 200 AND 300 ORDER BY id;
  +---+----+----+
  | id|name| age|
  +---+----+----+
  |200|Mary|null|
  |300|Mike|  80|
  +---+----+----+

-- Scalar Subquery in `WHERE` clause.
SELECT * FROM person WHERE age > (SELECT avg(age) FROM person);
  +---+----+---+
  |id |name|age|
  +---+----+---+
  |300|Mike|80 |
  +---+----+---+

-- Correlated column reference in `WHERE` clause of sub query.
SELECT * FROM person AS parent 
WHERE EXISTS (
              SELECT 1 FROM person AS child
              WHERE parent.id = child.id AND child.age IS NULL
             );
  +---+----+----+
  |id |name|age |
  +---+----+----+
  |200|Mary|null|
  +---+----+----+

{% endhighlight %}
### Related clauses
- [SELECT statement](sql-ref-syntax-qry-select.html)
- [WHERE clause](sql-ref-syntax-qry-select-where.html)
- [GROUP BY clause](sql-ref-syntax-qry-select-groupby.html)
- [HAVING clause](sql-ref-syntax-qry-select-having.html)
- [ORDER BY clause](sql-ref-syntax-qry-select-orderby.html)
- [SORT BY clause](sql-ref-syntax-qry-select-sortby.html)
- [CLUSTER BY clause](sql-ref-syntax-qry-select-clusterby.html)
- [DISTRIBUTE BY clause](sql-ref-syntax-qry-select-distribute-by.html)
- [WINDOW clause](sql-ref-syntax-qry-select-window.html)
- [LIMIT clause](sql-ref-syntax-qry-select-limit.html)
- [SET operators](sql-ref-syntax-qry-select-set-operators.html)
