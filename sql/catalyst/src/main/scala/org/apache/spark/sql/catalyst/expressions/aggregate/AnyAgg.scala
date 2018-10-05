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

import org.apache.spark.sql.catalyst.analysis.TypeCheckResult
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.util.TypeUtils
import org.apache.spark.sql.types._

@ExpressionDescription(
  usage = "_FUNC_(expr) - Returns true if at least one value of `expr` is true.")
case class AnyAgg(child: Expression) extends DeclarativeAggregate with ImplicitCastInputTypes {

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  override def dataType: DataType = BooleanType

  override def inputTypes: Seq[AbstractDataType] = Seq(BooleanType)

  override def checkInputDataTypes(): TypeCheckResult =
    TypeUtils.checkForBooleanExpr(child.dataType, "function any")

  private lazy val some = AttributeReference("some", BooleanType)()

  private lazy val valueSet = AttributeReference("valueSet", BooleanType)()

  override lazy val aggBufferAttributes = some :: valueSet :: Nil

  override lazy val initialValues: Seq[Expression] = Seq(
    /* some = */ Literal.create(false, BooleanType),
    /* valueSet = */ Literal.create(false, BooleanType)
  )

  override lazy val updateExpressions: Seq[Expression] = Seq(
    /* some = */ Or(some, If (child.isNull, some, child)),
    /* valueSet = */ valueSet || child.isNotNull
  )

  override lazy val mergeExpressions: Seq[Expression] = Seq(
    /* some = */ Or(some.left, some.right),
    /* valueSet */ valueSet.right || valueSet.left
  )

  override lazy val evaluateExpression: Expression =
    If (valueSet, some, Literal.create(null, BooleanType))
}
