# sesam-db2-iseries-as400-source
IBM DB2 iSeries AS400 source for Sesam.io powered applications

this service works on port 8080 and has one endpoint GET `/datasets/<TABLE NAME>/entities`

### environment variables needed
* **DB2_HOSTNAME** - hostname or IP to DB2 iSeries AS400 instance 
* **DB2_DBNAME** - database name 
* **DB2_USERNAME** - username
* **DB2_PASSWORD** - password
* **DB2_BATCH_SIZE** - how many rows fetches before data is written back to Sesam default 100000

### System setup 
```json
{
  "_id": "db2-test",
  "type": "system:microservice",
  "docker": {
    "environment": {
      "DB2_DBNAME": "<DB NAME>",
      "DB2_HOSTNAME": "<DB HOST OR IP>",
      "DB2_PASSWORD": "<DB PASSWORD>",
      "DB2_USERNAME": "<DB USER>"
    },
    "image": "<DOCKER IMAGE>",
    "memory": 512,
    "port": 8080
  }
}

```

### Pipe setup

```json
{
  "_id": "db2-test-pipe",
  "type": "pipe",
  "source": {
    "type": "json",
    "system": "db2-test",
    "supports_since": true,
    "url": "/datasets/<TABLE NAME>/entities?takeOnly=<columns to fetch, all if ommited>&sinceColumn=<column with last updated ts>"
  },
  "transform": {
    "type": "dtl",
    "rules": {
      "default": [
        ["copy", "*"],
        ["add", "_id",
          ["string", "_S.<PRIMARY KEY FIELD>"]
        ]
      ]
    }
  }
}
```
