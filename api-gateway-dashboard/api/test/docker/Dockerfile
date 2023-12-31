
FROM golang:1.19 AS build-env

WORKDIR /go/src/github.com/apisix/manager-api

COPY ./ ./

RUN mkdir -p /go/manager-api/conf \
    && go test -c -cover -covermode=atomic -o /go/manager-api/manager-api -coverpkg "./..." ./ \
    && mv /go/src/github.com/apisix/manager-api/entry.sh /go/manager-api/ \
    && mv /go/src/github.com/apisix/manager-api/conf/conf.yaml /go/manager-api/conf/conf.yaml \
    && mv /go/src/github.com/apisix/manager-api/conf/schema.json /go/manager-api/conf/schema.json \
    && mv /go/src/github.com/apisix/manager-api/conf/customize_schema.json /go/manager-api/conf/customize_schema.json \
    && rm -rf /go/src/github.com/apisix/manager-api \
    && rm -rf /etc/localtime \
    && ln -s  /usr/share/zoneinfo/Hongkong /etc/localtime \
    && dpkg-reconfigure -f noninteractive tzdata

RUN wget https://github.com/api7/dag-to-lua/archive/v1.1.tar.gz \
    && tar -zxvf v1.1.tar.gz \
    && mkdir -p /go/manager-api/dag-to-lua \
    && mv -u ./dag-to-lua-1.1/lib/* /go/manager-api/dag-to-lua/

FROM alpine:3.11

RUN mkdir -p /go/manager-api \
   && apk update  \
   && apk add ca-certificates \
   && update-ca-certificates \
   && apk add --no-cache libc6-compat \
   && echo "hosts: files dns" > /etc/nsswitch.conf \
   && rm -rf /var/cache/apk/*


WORKDIR /go/manager-api
COPY --from=build-env /go/manager-api/ /go/manager-api/
COPY --from=build-env /usr/share/zoneinfo/Hongkong /etc/localtime

RUN mkdir logs

EXPOSE 9000

RUN chmod +x ./entry.sh
ENTRYPOINT ["/go/manager-api/manager-api"]
CMD ["-test.coverprofile=./testdata/integrationcover.out"]
