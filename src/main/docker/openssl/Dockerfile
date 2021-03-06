# this is a dockerfile which builds OpenSSL with TLS 1.3 support,
# and start a local server
#
# the following command builds a docker image
# it should be run from the root of tlsbunny repository
#
# docker build --file src/main/docker/openssl/Dockerfile --tag openssl/server/tls13 .
#
# the following command starts an OpenSSL server
#
# docker run -p 10101:10101 openssl/server/tls13
#
# the following command starts an OpenSSL server with required client authentication
#
# docker run -p 10101:10101 -e OPTIONS="-Verify 0 -CAfile certs/root_cert.pem" openssl/server/tls13
#
# good luck!
#

FROM ubuntu

MAINTAINER artem.smotrakov@gmail.com

RUN apt-get update && apt-get install -y \
    gcc git make

RUN mkdir -p /var/src/openssl /var/src/tlsbunny

RUN git clone https://github.com/openssl/openssl /var/src/openssl && \
    cd /var/src/openssl && \
    ./config \
	    --prefix=/usr \
	    --debug \
	    enable-asan \
	    enable-tls1_3 \
	    enable-ssl-trace && \
	make && make install

EXPOSE 10101

ENV LD_LIBRARY_PATH /usr/local/lib

ADD . /var/src/tlsbunny
WORKDIR /var/src/tlsbunny

CMD [ "bash", "scripts/start_openssl_tls13_server.sh" ]
