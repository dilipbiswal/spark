---
layout: global
title: SELECT
displayTitle: SELECT 
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
Spark supports `SELECT` statement and conforms to ANSI SQL standard. Queries are
used to retrieve result sets from one or more table. The following section 
describes the overall query syntax and the sub-sections cover different constructs
of a query along with examples. 

### Syntax
{% highlight sql %}
[WITH with_query [, ...]]
SELECT [hints, ...] [ALL|DISTINCT] named_expression[, named_expression, ...]
  FROM from_item [, from_item, ...]
  [lateral_view [, lateral_view, ...]]
  [WHERE boolean_expression]
  [GROUP BY expression [, ...] ]
  [HAVING boolean_expression [, ...] ]
  [[ORDER | SORT ] BY expression [ ASC | DESC ] [ NULLS { FIRST | LAST } ] [, ...] ]
  [CLUSTER BY expressions]
  [DISTRIBUTE BY expressions]
  { UNION | INTERSECT | EXCEPT } [ ALL | DISTINCT ] select ]
  [WINDOW named_window[, WINDOW named_window, ...]]
  [LIMIT {ALL | expression}]

named_expression:
  : expression [AS alias]

relation:
  | join_relation
  | (table_name|query|relation) [sample] [AS alias]
  : VALUES (expressions)[, (expressions), ...]
        [AS (column_name[, column_name, ...])]

expressions:
  : expression[, expression, ...]

sort_expressions:
  : expression [ASC|DESC][, expression [ASC|DESC], ...]
{% endhighlight %}

### Parameters
<dl>
  <dt><code><em>with_query</em></code></dt>
  <dd>Optional common table expressions (CTEs) may be specified before the main <code>SELECT</code> query block. These table expressions
  are allowed to be referenced later in the main query. This is useful to abstract out repeated sub query blocks in the main query and
  improves readability of the query.</dd>
  <dt><code><em>hints</em></code></dt>
  <dd>Hints can be used to help spark optimizer make better planning decisions. Below are the supported list of hints:
    
  <dt><code><em>named_expression</em></code></dt>
  <dt><code><em>from_item</em></code></dt>
  <dt><code><em>lateral_view</em></code></dt>
  <dt><code><em>boolean_expression</em></code></dt>
  <dt><code><em>expression</em></code></dt>
  <dt><code><em>named_window</em></code></dt>
</dl>

