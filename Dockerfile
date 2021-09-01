FROM dockerhub.ir/openjdk:14-jdk-alpine
MAINTAINER HosseinHelali <hosseinhelali@gmail.com>

#Enviroment varaibles
ENV ZOOKEEPER_ADDRSS localhost:2181
ENV CLUSTER_NAME zorg_cluster
ENV NODE_NAME nos1.zorg.com


ENTRYPOINT [
    "java",
     "-jar",
    "/usr/share/binance-gateway/binance-gateway.jar" ,
    "--zorg.gateway.cluster.nam=${CLUSTER_NAME}",
    "--zorg.gateway.cluster.node.name=${NODE_NAME}",
    "--zorg.gateway.cluster.zk.nodes=${ZOOKEEPER_ADDRSS}"
]

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
#ADD target/lib           /usr/share/binance-gateway/lib


# Add the service itself
ARG JAR_FILE=target/*.jar
ADD target/${JAR_FILE} /usr/share/binance-gateway/binance-gateway.jar