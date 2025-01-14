/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package streaming.dsl.mmlib

import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.commons.codec.binary.Base64
import org.apache.http.HttpEntity
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPost}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.spark.sql._
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.lit

/**
 * Created by allwefantasy on 13/1/2018.
 */
trait SQLAlg extends Serializable {
  def train(df: DataFrame, path: String, params: Map[String, String]): DataFrame

  def load(sparkSession: SparkSession, path: String, params: Map[String, String]): Any

  def predict(sparkSession: SparkSession, _model: Any, name: String, params: Map[String, String]): UserDefinedFunction

  def batchPredict(df: DataFrame, path: String, params: Map[String, String]): DataFrame = {
    val sparkSession = df.sparkSession
    import sparkSession.implicits._
    Seq.empty[(String, String)].toDF("param", "description")
  }

  def explainParams(sparkSession: SparkSession): DataFrame = {
    import sparkSession.implicits._
    Seq.empty[(String, String)].toDF("param", "description")
  }

  def explainModel(sparkSession: SparkSession, path: String, params: Map[String, String]): DataFrame = {
    import sparkSession.implicits._
    Seq.empty[(String, String)].toDF("key", "value")
  }

  def scheduleAPI(df: DataFrame, params: Map[String, String], jsonObj: JSONObject, url: String) = {
    val client = HttpClients.createDefault()
    val httpPost = new HttpPost(url)
    httpPost.addHeader("Content-Type", "application/json;charset=UTF-8")
    httpPost.addHeader("Accept", "application/vnd.apache.kylin-v4-public+json")
    httpPost.addHeader("Accept-Language", "en")
    httpPost.addHeader("Authorization",
      "Basic " +
        new String(Base64.encodeBase64((params("username") + ":" + params("password")).getBytes())))
    httpPost.setEntity(new StringEntity(jsonObj.toJSONString))
    val response: CloseableHttpResponse = client.execute(httpPost)
    val entity: HttpEntity = response.getEntity
    val result = EntityUtils.toString(entity, "UTF-8")
    val jsonObject = JSON.parseObject(result)
    val code = jsonObject.getString("code")
    if (code.equals("000")) {
      df.withColumn("code", lit(jsonObject.getString("code"))).
        withColumn("data", lit(jsonObject.getJSONObject("data").toJSONString)).
        withColumn("msg", lit(jsonObject.getString("msg"))).
        select("code", "data", "msg")
    } else {
      df.withColumn("code", lit(jsonObject.getString("code"))).
        withColumn("msg", lit(jsonObject.getString("msg"))).
        select("code", "msg")
    }
  }

  def skipPathPrefix: Boolean = false

  def skipOriginalDFName: Boolean = true

  def modelType: ModelType = UndefinedType

  def doc: Doc = Doc(TextDoc, "")

  def codeExample: Code = Code(SQLCode, "")

  def coreCompatibility: Seq[CoreVersion] = {
    Seq(Core_2_4_x,Core_3_1_x)
  }

}

sealed abstract class ModelType
(
  val name: String,
  val humanFriendlyName: String
)

case object AlgType extends ModelType("algType", "algorithm")

case object ProcessType extends ModelType("processType", "feature engineer")

case object UndefinedType extends ModelType("undefinedType", "undefined")


case class Doc(docType: DocType, doc: String)

sealed abstract class DocType
(
  val docType: String
)

case object HtmlDoc extends DocType("html")

case object MarkDownDoc extends DocType("md")

case object TextDoc extends DocType("text")


case class Code(codeType: CodeType, code: String)

sealed abstract class CodeType
(
  val codeType: String
)

case object SQLCode extends CodeType("sql")

case object ScalaCode extends CodeType("scala")

case object PythonCode extends CodeType("python")


sealed abstract class CoreVersion
(
  val coreVersion: String
)

case object Core_2_2_x extends CoreVersion("2.2.x")

case object Core_2_3_1 extends CoreVersion("2.3.1")

case object Core_2_3_2 extends CoreVersion("2.3.2")

case object Core_2_3_x extends CoreVersion("2.3.x")

case object Core_2_4_x extends CoreVersion("2.4.x")

case object Core_3_0_x extends CoreVersion("3.0.x")

case object Core_3_1_x extends CoreVersion("3.1.x")




