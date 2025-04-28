#!/bin/bash

export JAVA_HOME="/usr/local/jdk1.8.0_381"
export MAVEN_HOME="/usr/local/apache-maven-3.6.3"
export PATH="$PATH:$MAVEN_HOME/bin:$JAVA_HOME/bin"

# 服务器ip
hostIp=$1
: ${hostIp:="127.0.0.1"}

# 项目编译打包
cd ../
mvn clean package -Dmaven.repo.local=$PWD/_repository -DskipTests -Dcheckstyle.skip=true
# 构建镜像
cd dataintegration-ui
# npm install node-sass --sass_binary_site=https://npm.taobao.org/mirrors/node-sass/
npm install --registry=http://registry.npmmirror.com
npm run build
cd ../

docker build -t dataintegration-group-provider dataintegration-group
docker build -t dataintegration-gateway dataintegration-gateway
docker build -t dataintegration-model-management-provider dataintegration-model
docker build -t dataintegration-project-provider dataintegration-project
docker build -t dataintegration-run-management-provider dataintegration-run
docker build -t dataintegration-sso-provider dataintegration-sso
docker build -t dataintegration-sys-management-provider dataintegration-sys
docker build -t dataintegration-ui dataintegration-ui
docker build -t dataintegration-file-management-provider dataintegration-file-management
# 启动应用
cd install && sed -i "s,hostIp,${hostIp},g" docker-compose.yaml
docker-compose up -d