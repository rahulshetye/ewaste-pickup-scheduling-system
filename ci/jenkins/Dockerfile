FROM jenkins/jenkins:lts
USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends chromium chromium-driver curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
# Docker CLI only (the daemon is the host's, reached through the mounted socket)
RUN curl -fsSL "https://download.docker.com/linux/static/stable/$(uname -m)/docker-27.3.1.tgz" \
    | tar xz --strip-components=1 -C /usr/local/bin docker/docker
USER jenkins
