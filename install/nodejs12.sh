#!/bin/bash

WORK_DIR=$HOME
# node-v16.19.1-linux-x64.tar.gz
tar="node-v12.22.10-linux-x64.tar.gz"

# 使用淘宝镜像安装Node.js v16
wget -nc  https://registry.npmmirror.com/-/binary/node/latest-v12.x/$tar -O $WORK_DIR/$tar && \
    sudo tar -C /usr/local --strip-components 1 -xzf $WORK_DIR/$tar && \
    # 安装cnpm
    sudo npm i -g cnpm --registry=http://registry.npmmirror.com && \
    # install yarn
    sudo cnpm install -g yarn react-native-cli && \
    sudo yarn config set registry http://registry.npmmirror.com --global && \
    sudo yarn config set disturl http://registry.npmmirror.com/dist --global && \
    sudo npm config set registry http://registry.npmmirror.com