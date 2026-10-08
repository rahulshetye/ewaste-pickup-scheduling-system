pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'staging'],
               description: 'Target environment (decides which port the app runs on)')
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        TARGET_ENV  = "${params.DEPLOY_ENV ?: 'dev'}"
        SERVER_PORT = "${(params.DEPLOY_ENV ?: 'dev') == 'staging' ? '8083' : '8082'}"
        DEPLOY_DIR  = "/var/jenkins_home/deploy/${params.DEPLOY_ENV ?: 'dev'}"
        DB_URL      = 'jdbc:mysql://host.docker.internal:3306/ewaste_pickup'
        TEST_PORT   = '8089'
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

        // Quality gate: if anything in this stage fails, Deploy and Verify never run.
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

        stage('Deploy') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'ewaste-db',
                                                  usernameVariable: 'DB_USERNAME',
                                                  passwordVariable: 'DB_PASSWORD')]) {
                    sh '''
                        mkdir -p "$DEPLOY_DIR"
                        if [ -f "$DEPLOY_DIR/app.pid" ]; then
                            kill "$(cat "$DEPLOY_DIR/app.pid")" || true
                            sleep 5
                        fi
                        cp target/ewaste-pickup-*.jar "$DEPLOY_DIR/app.jar"
                        cd "$DEPLOY_DIR"
                        JENKINS_NODE_COOKIE=dontKillMe nohup java -jar app.jar > app.log 2>&1 &
                        echo $! > app.pid
                    '''
                }
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    for i in $(seq 1 30); do
                        if curl -sf "http://localhost:$SERVER_PORT/api/slots" > /dev/null; then
                            echo "App is up on port $SERVER_PORT ($TARGET_ENV)"
                            exit 0
                        fi
                        sleep 2
                    done
                    echo "App did not start. Last log lines:"
                    tail -50 "$DEPLOY_DIR/app.log"
                    exit 1
                '''
            }
        }
    }
}
