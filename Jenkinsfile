pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'staging'],
               description: 'Target environment (dev = host port 8092, staging = 8093)')
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        TARGET_ENV     = "${params.DEPLOY_ENV ?: 'dev'}"
        HOST_PORT      = "${(params.DEPLOY_ENV ?: 'dev') == 'staging' ? '8093' : '8092'}"
        CONTAINER_NAME = "ewaste-${params.DEPLOY_ENV ?: 'dev'}"
        DB_URL         = 'jdbc:mysql://host.docker.internal:3306/ewaste_pickup'
        TEST_PORT      = '8089'
        IMAGE_REPO     = 'localhost:5001/ewaste-pickup'
        REGISTRY_HOST  = 'host.docker.internal:5001'
        VERSION        = "1.0.${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean compile'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/ewaste-pickup-*.jar', fingerprint: true
            }
        }

        // Quality gate: if anything in this stage fails, nothing after it runs.
        stage('Selenium UI Tests') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'ewaste-db',
                                                  usernameVariable: 'DB_USERNAME',
                                                  passwordVariable: 'DB_PASSWORD')]) {
                    sh '''
                        rm -f test-app.pid
                        SERVER_PORT=$TEST_PORT nohup java -jar target/ewaste-pickup-*.jar > test-app.log 2>&1 &
                        echo $! > test-app.pid
                        for i in $(seq 1 40); do
                            if curl -sf "http://localhost:$TEST_PORT/api/slots" > /dev/null; then
                                echo "Test instance is up on port $TEST_PORT"
                                exit 0
                            fi
                            sleep 2
                        done
                        echo "Test instance did not start. Last log lines:"
                        tail -50 test-app.log
                        exit 1
                    '''
                }
                sh '''
                    mvn -B test -Dtest=WebUiJourneysTest \
                        -DbaseUrl=http://localhost:$TEST_PORT \
                        -Dheadless=true -DrequireApp=true \
                        -Dwebdriver.chrome.driver=/usr/bin/chromedriver \
                        -DchromeBinary=/usr/bin/chromium
                '''
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'target/screenshots/*.png, test-app.log', allowEmptyArchive: true
                    sh '''
                        if [ -f test-app.pid ]; then
                            kill "$(cat test-app.pid)" || true
                            rm -f test-app.pid
                        fi
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                }
                sh '''
                    docker build \
                        --label org.opencontainers.image.revision=$GIT_SHORT \
                        --label org.opencontainers.image.version=$VERSION \
                        --label ci.build.number=$BUILD_NUMBER \
                        -t $IMAGE_REPO:$VERSION \
                        -t $IMAGE_REPO:$GIT_SHORT \
                        -t $IMAGE_REPO:latest .
                    docker images $IMAGE_REPO
                '''
            }
        }

        stage('Push to Registry') {
            steps {
                sh '''
                    docker push $IMAGE_REPO:$VERSION
                    docker push $IMAGE_REPO:$GIT_SHORT
                    docker push $IMAGE_REPO:latest
                    echo "--- registry catalog ---"
                    curl -s http://$REGISTRY_HOST/v2/_catalog
                    echo
                    echo "--- registry tags ---"
                    curl -s http://$REGISTRY_HOST/v2/ewaste-pickup/tags/list
                    echo
                '''
            }
        }

        stage('Deploy Container') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'ewaste-db',
                                                  usernameVariable: 'DB_USERNAME',
                                                  passwordVariable: 'DB_PASSWORD')]) {
                    sh '''
                        docker pull $IMAGE_REPO:$VERSION
                        docker rm -f $CONTAINER_NAME || true
                        docker run -d --name $CONTAINER_NAME --restart unless-stopped \
                            -p $HOST_PORT:8080 \
                            -e DB_URL -e DB_USERNAME -e DB_PASSWORD \
                            $IMAGE_REPO:$VERSION
                        docker ps --filter name=$CONTAINER_NAME
                    '''
                }
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    for i in $(seq 1 40); do
                        if curl -sf "http://host.docker.internal:$HOST_PORT/api/slots" > /dev/null; then
                            echo "Container $CONTAINER_NAME is up on host port $HOST_PORT ($TARGET_ENV)"
                            echo "Image: $(docker inspect $CONTAINER_NAME --format '{{.Config.Image}}')"
                            echo "Commit label: $(docker inspect $CONTAINER_NAME --format '{{index .Config.Labels "org.opencontainers.image.revision"}}')"
                            exit 0
                        fi
                        sleep 2
                    done
                    echo "Container did not become healthy. Last log lines:"
                    docker logs --tail 50 $CONTAINER_NAME
                    exit 1
                '''
            }
        }
    }
}
