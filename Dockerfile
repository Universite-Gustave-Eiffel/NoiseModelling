# Use a Maven image that already includes JDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /build

# Install bsdtar (found in libarchive-tools on Debian/Ubuntu-based images)
RUN apt-get update && apt-get install -y libarchive-tools && rm -rf /var/lib/apt/lists/*

# Copy your project files
COPY . /build/

# Run your commands
RUN mvn package && \
    mkdir noisemodelling && \
    bsdtar -xvf covadis-webserver/target/*.zip --strip-components=1 -C noisemodelling

FROM eclipse-temurin:latest

LABEL org.opencontainers.image.authors="Nicolas Fortin, UMRAE Univ Eiffel, contact@noise-planet.org"

LABEL org.opencontainers.image.description="NoiseModelling server mode distribution. See https://github.com/Universite-Gustave-Eiffel/NoiseModelling/blob/main/README.md for more information"

ENV NOISEMODELLING_GID=8978
ENV NOISEMODELLING_UID=8978
ENV APP_DIR=/srv/noisemodelling/
ENV APP_FILE=$APP_DIR/bin/WebServer

RUN <<EOF
groupadd -g $NOISEMODELLING_GID noisemodelling && \
useradd -g $NOISEMODELLING_GID -m -u $NOISEMODELLING_UID -s /bin/bash noisemodelling
EOF

WORKDIR $APP_DIR

COPY --from=builder /build/noisemodelling/ $APP_DIR

RUN <<EOF
chmod +x "$APP_FILE"
mkdir $APP_DIR/workspace
chown -R noisemodelling:noisemodelling $APP_DIR/workspace
EOF

# Switch to the non-root user
USER noisemodelling

EXPOSE 8000

ENTRYPOINT ["sh", "-c", "exec /srv/noisemodelling/bin/WebServer -b -w /srv/noisemodelling/workspace -s /srv/noisemodelling/scripts ${PROXY_BASE_URL:+-l \"$PROXY_BASE_URL\"} ${ROOT_URL+-r \"$ROOT_URL\"} ${UNSECURE_MODE+-u} ${DB_ENCRYPTION_SECRET:+-e \"$DB_ENCRYPTION_SECRET\"}"]
