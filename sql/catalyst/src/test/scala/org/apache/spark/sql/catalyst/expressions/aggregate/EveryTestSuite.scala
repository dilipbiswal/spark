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
package org.apache.spark.sql.catalyst.expressions.aggregate

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{AttributeReference, Literal}
import org.apache.spark.sql.types.BooleanType

class EveryTestSuite extends SparkFunSuite {
  val input = AttributeReference("input", BooleanType, nullable = true)()
  val evaluator = DeclarativeAggregateEvaluator(Every(input), Seq(input))

  test("empty buffer") {
    assert(evaluator.initialize() === InternalRow(true, false))
  }

  test("update") {
    val result = evaluator.update(
      InternalRow(true),
      InternalRow(false),
      InternalRow(true))
    assert(result === InternalRow(false, true))
  }

  test("merge") {
    // Empty merge
    val p0 = evaluator.initialize()
    assert(evaluator.merge(p0) === InternalRow(true, false))

    // Single merge
    val p1 = evaluator.update(InternalRow(true), InternalRow(true))
    assert(evaluator.merge(p1) === InternalRow(true, true))

    // Multiple merges.
    val p2 = evaluator.update(InternalRow(true), InternalRow(null))
    assert(evaluator.merge(p1, p2) === InternalRow(true, true))

    // Empty partitions (p0 is empty)
    assert(evaluator.merge(p1, p0, p2) === InternalRow(true, true))
    assert(evaluator.merge(p2, p1, p0) === InternalRow(true, true))
  }

  test("eval") {
    // Null Eval
    assert(evaluator.eval(InternalRow(true, false)) === InternalRow(null))
    assert(evaluator.eval(InternalRow(false, false)) === InternalRow(null))

    // Empty Eval
    val p0 = evaluator.initialize()
    assert(evaluator.eval(p0) === InternalRow(null))

    // Update - Eval
    val p1 = evaluator.update(InternalRow(true), InternalRow(true))
    assert(evaluator.eval(p1) === InternalRow(true))

    // Update - Merge - Eval
    val p2 = evaluator.update(InternalRow(false), InternalRow(true))
    val m1 = evaluator.merge(p1, p0, p2)
    assert(evaluator.eval(m1) === InternalRow(false))

    // Update - Merge - Eval (empty partition at the end)
    val m2 = evaluator.merge(p2, p1, p0)
    assert(evaluator.eval(m2) === InternalRow(false))
  }

}
