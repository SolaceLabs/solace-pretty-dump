# To BUILD:
#
#     1) ./gradlew clean assemble      (or download a pre-built release from GitHub)
#     2) cd build/distributions
#     3) unzip prettydump.zip
#     4) cd ../..
#     5) docker build -t solace-pretty-dump:<version> --file Dockerfile .
#
#
# To RUN:
#
#     docker run -it --rm -e "TERM=xterm-256color" solace-pretty-dump:latest broker.messaging.solace.cloud vpn-name user pw ">" -1
#
#
# To RUN in "wrap" mode:  (can't use -t pseudo-TTY mode since pipe | doesn't work
#
#     ./sdkperf_java.sh -cip=0 -sql=q1 -md | docker run -i --rm solace-pretty-dump:latest wrap
#
#    (maybe one day something like this could work: https://stackoverflow.com/questions/1401002/how-to-trick-an-application-into-thinking-its-stdout-is-a-terminal-not-a-pipe )



# Go and make a custom JRE in the distribution lib folder:
# jlink --add-modules ALL-MODULE-PATH --strip-debug --no-man-pages --no-header-files --compress=2  --output ../jre

# greetings.Dockerfile

#FROM amazoncorretto:17-alpine as corretto-jdk
FROM amazoncorretto:22-alpine as corretto-jdk

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
COPY build/distributions/prettydump/ ./

ENTRYPOINT ["./bin/prettydump"] 

