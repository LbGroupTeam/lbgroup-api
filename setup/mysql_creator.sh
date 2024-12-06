docker run \
-e MYSQL_ROOT_PASSWORD=root \
-e MYSQL_DATABASE=human_finance \
-p 3306:3306 \
-v ./src/test/resources/test_database_dump.sql:/docker-entrypoint-initdb.d/init.sql \
-d mysql:latestdocker