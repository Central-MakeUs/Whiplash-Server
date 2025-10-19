// Jenkinsfile
pipeline {
    agent any

    tools {
        jdk 'jdk17' // Jenkins Tools 설정에 등록된 JDK 이름
    }

    stages {
        // 1. 소스 코드 체크아웃
        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: 'GITHUB_TOKEN', // GitHub PAT Credential ID
                    url: 'https://github.com/Central-MakeUs/Whiplash-Server.git'
            }
        }

        // 2. Git Commit SHA 추출 (Docker 이미지 태그로 사용)
        stage('Extract SHA') {
            steps {
                script {
                    env.SHORT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo "Current commit SHA: ${env.SHORT_SHA}"
                }
            }
        }

        // withCredentials 블록으로 Jenkins에 등록된 모든 Secret 값을 변수로 로드
        stage('Run CI/CD with Credentials') {
            steps {
                withCredentials([
                    // Docker & Server Info
                    string(credentialsId: 'DOCKERHUB_USERNAME', variable: 'DOCKERHUB_USERNAME'),
                    string(credentialsId: 'IMAGE_NAME', variable: 'IMAGE_NAME'),
                    string(credentialsId: 'PROD_HOST', variable: 'PROD_HOST'),
                    string(credentialsId: 'PROD_USERNAME', variable: 'PROD_USERNAME'),
                    // Application Config Files
                    string(credentialsId: 'PROD_ENV_PROPERTIES', variable: 'ENV_PROPERTIES'),
                    string(credentialsId: 'PROD_GOOGLE_JSON_BASE64', variable: 'GOOGLE_JSON_B64'),
                    string(credentialsId: 'PROD_FIREBASE_KEY_JSON_BASE64', variable: 'FIREBASE_KEY_B64')
                ]) {
                    script {
                        // 3. 운영 환경 설정 파일 생성
                        stage('Generate Config Files') {
                            sh '''
                                mkdir -p src/main/resources
                                echo "${ENV_PROPERTIES}" > src/main/resources/env.properties
                                echo "${GOOGLE_JSON_B64}" | base64 -d > src/main/resources/google.json
                                echo "${FIREBASE_KEY_B64}" | base64 -d > src/main/resources/whiplash-firebase-key.json
                            '''
                        }

                        // 4. Gradle 빌드
                        stage('Build') {
                            sh 'chmod +x ./gradlew'
                            sh './gradlew bootJar --no-daemon'
                        }

                        // 5. Docker 이미지 빌드 및 푸시
                        stage('Build and Push Docker Image') {
                            withCredentials([usernamePassword(credentialsId: 'DOCKERHUB_CREDENTIALS', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                                sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                                sh "docker build -t ${IMAGE_NAME}:${env.SHORT_SHA} ."
                                sh "docker push ${IMAGE_NAME}:${env.SHORT_SHA}"
                            }
                        }

                        // 6. 운영 서버에 무중단 배포 실행
                        stage('Deploy Blue/Green to Production') {
                            sshagent(credentials: ['PROD_PRIVATE_KEY']) {
                                // deploy.sh 스크립트에 이미지 태그와 이미지 이름을 인자로 전달
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${PROD_USERNAME}@${PROD_HOST} 'cd /opt/app/scripts && ./deploy.sh ${env.SHORT_SHA} ${IMAGE_NAME}'
                                """
                            }
                        }
                    }
                }
            }
        }
    }

    // 파이프라인 종료 후 실행될 작업 (성공/실패 알림)
    post {
        always {
            script {
                withCredentials([string(credentialsId: 'DISCORD_WEBHOOK_URL', variable: 'HOOK_URL')]) {
                    def message
                    def color

                    // currentBuild.result는 SUCCESS, UNSTABLE, FAILURE 등의 값을 가짐
                    if (currentBuild.result == 'SUCCESS') {
                        color = '3066993' // Green
                        message = "✅ **[${env.JOB_NAME}]** 빌드 성공! ( #${env.BUILD_NUMBER} )"
                    } else if (currentBuild.result == 'UNSTABLE') {
                        color = '15105570' // Yellow
                        message = "⚠️ **[${env.JOB_NAME}]** 빌드 불안정. ( #${env.BUILD_NUMBER} )"
                    } else {
                        color = '15158332' // Red
                        message = "❌ **[${env.JOB_NAME}]** 빌드 실패! ( #${env.BUILD_NUMBER} )"
                    }

                    // Discord Notifier 플러그인의 discordSend 스텝 사용
                    discordSend(
                        webhookURL: "${HOOK_URL}",
                        embeds: [[
                            "title": "Build Status: ${currentBuild.result}",
                            "description": "${message}\n[Jenkins 빌드 로그 보기](${env.BUILD_URL})",
                            "color": color
                        ]]
                    )
                }
            }
            // 정리 작업
            echo 'Pipeline finished. Cleaning up...'
            sh 'docker logout'
        }
    }
}