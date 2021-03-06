package ho.mi

/* This program reads data from JSON file containing nested array and using it created 2 tables in the Postgres database */

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object Personidata {
  def main(args: Array[String]) {

  val spark = SparkSession
      .builder
      .master("local")	
      .appName("PersonData")
      .getOrCreate()
	  
	// Registering Driver - Might be redundant in some cases  
	Class.forName("org.postgresql.Driver").newInstance

	//Set database connection properties
	val dbProperties = new java.util.Properties
	dbProperties.load(new java.io.FileInputStream(new java.io.File("db-properties.flat")));
	val jdbcUrl = dbProperties.getProperty("jdbcUrl")

	
	/*
	The Following code using Flattening feature displays nested array elements in JSON Array along with Non-array elements
	Note that while diplaying the results as there are multiple values of Array fileds with single value, too will appear repetedly multiple times
		
	val df_personNew=spark.read.json("personNew.json")
	df_personNew.registerTempTable("personNew")
	spark.sql("select created,created_by,mi_identity_handles.interface_identifier,mi_identity_handles.unique_identifier,
	mi_identity_handles.visibility_marker,internal_handle.interface_identifier,internal_handle.unique_identifier,internal_handle.visibility_marker,
	person_space from personJson LATERAL VIEW explode(identity_handles) as mi_identity_handles").show(false)
	
	*/
	
	//val df_id=spark.read.format("json").option("header", "true").load("person.json")
	val df_person=spark.read.json("person.json")
	
	//Flatten the array in JSON and load into "Identity" table
	val df_id_handle=df_person.select(explode(df_person("identity_handles"))).toDF("identity_handles")
	val df_id_handle_flatten=df_id_handle.select("identity_handles.unique_identifier","identity_handles.visibility_marker","identity_handles.interface_identifier")

	//Put all elements in JSON, except the "identity_handles" into "Person" table
	val df_person_data=df_person.select("internal_handle.visibility_marker", "internal_handle.interface_identifier", "person_space", "created", "created_by")
	
	
	// Creating table 'Person' in the Postgres database	
	var tab_name1="person_details"
	df_person_data.write.mode("error").jdbc(jdbcUrl, tab_name1, dbProperties)

	// Creating table 'Identity' in the Postgres database	
	var tab_name2="identity"
	df_id_handle_flatten.write.mode("overwrite").jdbc(jdbcUrl, tab_name2, dbProperties)


   spark.stop()
  }
}	  



