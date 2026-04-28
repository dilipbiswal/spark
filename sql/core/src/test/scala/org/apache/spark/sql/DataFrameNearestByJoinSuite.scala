/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql

import org.apache.spark.sql.catalyst.plans.{NearestByDirection, NearestByJoinMode, NearestByJoinType}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.tags.SlowSQLTest

@SlowSQLTest
class DataFrameNearestByJoinSuite extends QueryTest with SharedSparkSession {

  private def prepareForNearestByJoin(): (classic.DataFrame, classic.DataFrame) = {
    val users = spark.createDataFrame(
      Seq((1, 10.0), (2, 20.0), (3, 30.0))).toDF("user_id", "score")
    val products = spark.createDataFrame(
      Seq(("A", 11.0), ("B", 22.0), ("C", 5.0))).toDF("product", "pscore")
    (users, products)
  }

  test("similarity, inner, k=1") {
    val (users, products) = prepareForNearestByJoin()
    val result = users.nearestByJoin(
      products,
      -abs(users("score") - products("pscore")),
      numResults = 1,
      direction = "similarity")

    checkAnswer(
      result.select("user_id", "product").orderBy("user_id"),
      Seq(Row(1, "A"), Row(2, "B"), Row(3, "B"))
    )
  }

  test("distance, inner, k=2") {
    val (users, products) = prepareForNearestByJoin()
    val result = users.nearestByJoin(
      products,
      abs(users("score") - products("pscore")),
      numResults = 2,
      direction = "distance")

    // For each user_id, closest 2 by |score - pscore|:
    //   user 1 (10): A (|10-11|=1), C (|10-5|=5)
    //   user 2 (20): B (|20-22|=2), A (|20-11|=9)
    //   user 3 (30): B (|30-22|=8), A (|30-11|=19)
    checkAnswer(
      result.select("user_id", "product").orderBy("user_id", "product"),
      Seq(
        Row(1, "A"), Row(1, "C"),
        Row(2, "A"), Row(2, "B"),
        Row(3, "A"), Row(3, "B"))
    )
  }

  test("left outer when right side is empty") {
    val (users, products) = prepareForNearestByJoin()
    val emptyProducts = products.filter(lit(false))
    val result = users.nearestByJoin(
      emptyProducts,
      -abs(users("score") - emptyProducts("pscore")),
      numResults = 1,
      joinType = "leftouter",
      mode = "approx",
      direction = "similarity")

    checkAnswer(
      result.select("user_id", "product").orderBy("user_id"),
      Seq(Row(1, null), Row(2, null), Row(3, null))
    )
  }

  test("inner drops left rows with no matches") {
    val (users, products) = prepareForNearestByJoin()
    val emptyProducts = products.filter(lit(false))
    val result = users.nearestByJoin(
      emptyProducts,
      -abs(users("score") - emptyProducts("pscore")),
      numResults = 1,
      direction = "similarity")

    assert(result.count() === 0)
  }

  test("SQL: APPROX NEAREST SIMILARITY") {
    val (users, products) = prepareForNearestByJoin()
    users.createOrReplaceTempView("t_users")
    products.createOrReplaceTempView("t_products")
    try {
      val result = spark.sql(
        """
          |SELECT u.user_id, p.product
          |FROM t_users u JOIN t_products p
          |  APPROX NEAREST 1 BY SIMILARITY -abs(u.score - p.pscore)
          |""".stripMargin)
      checkAnswer(
        result.orderBy("user_id"),
        Seq(Row(1, "A"), Row(2, "B"), Row(3, "B"))
      )
    } finally {
      spark.catalog.dropTempView("t_users")
      spark.catalog.dropTempView("t_products")
    }
  }

  test("SQL: EXACT NEAREST DISTANCE") {
    val (users, products) = prepareForNearestByJoin()
    users.createOrReplaceTempView("t_users")
    products.createOrReplaceTempView("t_products")
    try {
      val result = spark.sql(
        """
          |SELECT u.user_id, p.product
          |FROM t_users u JOIN t_products p
          |  EXACT NEAREST 1 BY DISTANCE abs(u.score - p.pscore)
          |""".stripMargin)
      checkAnswer(
        result.orderBy("user_id"),
        Seq(Row(1, "A"), Row(2, "B"), Row(3, "B"))
      )
    } finally {
      spark.catalog.dropTempView("t_users")
      spark.catalog.dropTempView("t_products")
    }
  }

  test("invalid numResults is rejected") {
    val (users, products) = prepareForNearestByJoin()
    Seq(0, 100001).foreach { k =>
      checkError(
        exception = intercept[AnalysisException] {
          users.nearestByJoin(
            products,
            -abs(users("score") - products("pscore")),
            numResults = k,
            direction = "similarity")
        },
        condition = "NEAREST_BY_JOIN.NUM_RESULTS_OUT_OF_RANGE",
        parameters = Map(
          "numResults" -> k.toString,
          "min" -> "1",
          "max" -> "100000"))
    }
  }

  test("invalid joinType is rejected") {
    val (users, products) = prepareForNearestByJoin()
    checkError(
      exception = intercept[AnalysisException] {
        users.nearestByJoin(
          products,
          -abs(users("score") - products("pscore")),
          numResults = 1,
          joinType = "rightouter",
          mode = "approx",
          direction = "similarity")
      },
      condition = "NEAREST_BY_JOIN.UNSUPPORTED_JOIN_TYPE",
      parameters = Map(
        "joinType" -> "rightouter",
        "supported" -> NearestByJoinType.supported.mkString("'", "', '", "'")))
  }

  test("invalid mode is rejected") {
    val (users, products) = prepareForNearestByJoin()
    checkError(
      exception = intercept[AnalysisException] {
        users.nearestByJoin(
          products,
          -abs(users("score") - products("pscore")),
          numResults = 1,
          joinType = "inner",
          mode = "bogus",
          direction = "similarity")
      },
      condition = "NEAREST_BY_JOIN.UNSUPPORTED_MODE",
      parameters = Map(
        "mode" -> "bogus",
        "supported" -> NearestByJoinMode.supported.mkString("'", "', '", "'")))
  }

  test("invalid direction is rejected") {
    val (users, products) = prepareForNearestByJoin()
    checkError(
      exception = intercept[AnalysisException] {
        users.nearestByJoin(
          products,
          -abs(users("score") - products("pscore")),
          numResults = 1,
          direction = "bogus")
      },
      condition = "NEAREST_BY_JOIN.UNSUPPORTED_DIRECTION",
      parameters = Map(
        "direction" -> "bogus",
        "supported" -> NearestByDirection.supported.mkString("'", "', '", "'")))
  }

  test("non-orderable ranking expression is rejected") {
    val (users, products) = prepareForNearestByJoin()
    checkError(
      exception = intercept[AnalysisException] {
        users.nearestByJoin(
          products,
          map(users("score"), products("pscore")),
          numResults = 1,
          direction = "similarity")
      },
      condition = "NEAREST_BY_JOIN.NON_ORDERABLE_RANKING_EXPRESSION",
      parameters = Map(
        "expression" -> "\"map(score, pscore)\"",
        "type" -> "\"MAP<DOUBLE, DOUBLE>\""))
  }

  test("EXACT mode rejects nondeterministic ranking expression") {
    val (users, products) = prepareForNearestByJoin()
    checkError(
      exception = intercept[AnalysisException] {
        users.nearestByJoin(
          products,
          rand() + products("pscore"),
          numResults = 1,
          joinType = "inner",
          mode = "exact",
          direction = "similarity")
      },
      condition = "NEAREST_BY_JOIN.EXACT_WITH_NONDETERMINISTIC_EXPRESSION",
      matchPVals = true,
      parameters = Map("expression" -> ".*rand.*pscore.*"))
  }
}
