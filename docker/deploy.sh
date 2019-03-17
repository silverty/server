#!/bin/sh

mysql_docker_name="wfc-mysql"
java_docker_name="wfc-java"
docker stop "$mysql_docker_name"
docker stop "$java_docker_name"
mysql_data_dir="mysql_data"
echo $mysql_data_dir
mpserver_dir="mpserver"
/bin/rm -rf "$mpserver_dir"
mysql_docker_data_path="$(pwd)/$mysql_data_dir"
wfc_sql_path="$(pwd)/../broker/sql/"
wfc_app_path="$(pwd)/app/"
echo $wfc_docker_path

docker run -d --rm --name $mysql_docker_name  \
	-v $mysql_docker_data_path:/var/lib/mysql \
	-v $wfc_sql_path:/sql:rw \
	-e MYSQL_USER=root \
	-e MYSQL_ROOT_PASSWORD=123456 \
	-e MYSQL_DATABASE=wfchat mysql:5.7.18 \
	--character-set-server=utf8mb4 \
	--collation-server=utf8mb4_unicode_ci \
	--init_connect='SET NAMES utf8mb4' \
	--character-set-client-handshake=FALSE 
echo "to sleep 30s"
sleep 30
#docker exec -it $mysql_docker_name /bin/bash -c ' cd /sql/ && ./initialize_db.sh -uroot -p123456 '

docker run -d --rm -it --name $java_docker_name \
	--link $mysql_docker_name:mysql \
	-v $wfc_app_path:/app:rw \
	-p 1983:1983 \
	-p 1883:1883 \
	-p 8083:8083 \
	--entrypoint /bin/bash java -c 'cd /app/bin && nohup sh moquette.sh '
echo "to sleep 30s"
sleep 30
docker ps


