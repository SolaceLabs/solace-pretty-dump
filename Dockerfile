# Download a Docker tarball from https://github.com/SolaceLabs/solace-pretty-dump/releases
# Load into Docker Images with: docker load -i solace-pretty-dump-1.1.0.tar.gz
#
# To RUN:
#
#     docker run -it --rm solace-pretty-dump:latest broker.messaging.solace.cloud vpn-name user pw ">" -1
#
# or make an alias: pretty='docker run -it --rm solace-pretty-dump:latest'
#
#
# To RUN in "wrap" mode around SdkPerf:  (can't use -t pseudo-TTY mode since pipe | doesn't work
#
#     ./sdkperf_java.sh -cip=0 -sql=q1 -md | docker run -i --rm solace-pretty-dump:latest wrap
#
#    (maybe one day something like this could work: https://stackoverflow.com/questions/1401002/how-to-trick-an-application-into-thinking-its-stdout-is-a-terminal-not-a-pipe )





# To BUILD:
#
#     1) ./gradlew clean assemble      (or download a pre-built release from GitHub)
#     2) docker build -t solace-pretty-dump:latest -t solace-pretty-dump:1.1.0 --file Dockerfile .
#
# Optional, for distribution:
#
#     3) docker save solace-pretty-dump:latest | gzip > solace-pretty-dump-x.y.z.tar.gz
#


# This is where the actual Dockerfile starts

FROM amazoncorretto:22-alpine as corretto-jdk
#FROM amazoncorretto:17-alpine as corretto-jdk

# required for strip-debug to work
RUN apk add --no-cache binutils

# Build small JRE image
RUN jlink \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /jre

FROM alpine:latest
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=corretto-jdk /jre $JAVA_HOME

RUN mkdir -p /opt/pretty
WORKDIR /opt/pretty

# after doing ./gradlew assemble, unzip a distribution, and then this will copy all the build/staged into the Docker image
COPY build/staged/ ./

ENTRYPOINT ["./bin/prettydump"] 

ENV TERM="xterm-256color"

LABEL version=1.1.0
LABEL author="Aaron @ Solace"
LABEL repo="https://github.com/SolaceLabs/solace-pretty-dump"
