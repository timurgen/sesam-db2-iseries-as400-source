# sesam-db2-iseries-as400-source
IBM DB2 iSeries AS400 source for Sesam.io powered applications

this service works on port 8080 and has one endpoint GET `/datasets/<TABLE NAME>/entities`

### environment variables needed
* **DB2_HOSTNAME** - hostname or IP to DB2 iSeries AS400 instance 
* **DB2_DBNAME** - database name 
* **DB2_USERNAME** - username
* **DB2_PASSWORD** - password
