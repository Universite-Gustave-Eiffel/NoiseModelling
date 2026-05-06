This tutorial cover the computation of Lday, Levening, lnight and Lden noise levels from road traffic.
 
 The results csv files are placed into the target subdirectory. Its contains the levels for each specified evaluation point.
 
 # Run unit test with PostGIS database

Launch this docker container to have a PostGIS database ready for unit tests:

```bash
docker run -d --name noisemodelling-postgres \
-p 5432:5432 \
-e POSTGRES_USER=noisemodelling \
-e POSTGRES_PASSWORD=noisemodelling \
-e POSTGRES_DB=noisemodelling_db \
--health-cmd='pg_isready' \
--health-interval=10s \
--health-timeout=5s \
--health-retries=5 \
postgis/postgis:16-3.4
```

Then compile with maven and run the unit tests:

```bash
mvn clean test
```

After the tests, you can stop and remove the container:

```bash
docker stop noisemodelling-postgres
docker rm noisemodelling-postgres
```