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
                    string(credentialsId: 'PROD_WAS_HOST', variable: 'PROD_WAS_HOST'),
                    string(credentialsId: 'PROD_USERNAME', variable: 'PROD_USERNAME'),
                    string(credentialsId: 'PROD_WAS_SSH_PORT', variable: 'PROD_WAS_SSH_PORT'),
                    // Application Config Files
                    file(credentialsId: 'PROD_ENV_PROPERTIES', variable: 'ENV_PROPERTIES_FILE_PATH'),
                    string(credentialsId: 'PROD_GOOGLE_JSON_BASE64', variable: 'GOOGLE_JSON_B64'),
                    string(credentialsId: 'PROD_FIREBASE_KEY_JSON_BASE64', variable: 'FIREBASE_KEY_B64')
                ]) {
                    script {
                        // 3. 운영 환경 설정 파일 생성
                        stage('Generate Config Files') {
                            sh '''
                                mkdir -p src/main/resources
                                # ENV_PROPERTIES_FILE_PATH 변수에는 임시 파일의 경로가 담겨있음
                                cp "${ENV_PROPERTIES_FILE_PATH}" src/main/resources/env.properties
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
                            // sshagent를 withCredentials 리스트가 아닌, 중첩된 래퍼(wrapper)로 올바르게 사용
                            // 먼저 withCredentials로 Docker 인증 정보를 변수로 로드
                            withCredentials([usernamePassword(credentialsId: 'DOCKERHUB_CREDENTIALS', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                                // 그 안에서 sshagent로 SSH 환경을 감싸줌
                                sshagent(credentials: ['PROD_PRIVATE_KEY']) {
                                    // DOCKER_USER/PASS 변수와 SSH 키를 모두 사용 가능
                                    sh """
                                        ssh -p ${PROD_WAS_SSH_PORT} -o StrictHostKeyChecking=no ${PROD_USERNAME}@${PROD_WAS_HOST} 'cd /opt/app/scripts && ./deploy.sh "${env.SHORT_SHA}" "${IMAGE_NAME}" "${DOCKER_USER}" "${DOCKER_PASS}"'
                                    """
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                withCredentials([string(credentialsId: 'DISCORD_WEBHOOK_URL', variable: 'HOOK_URL')]) {
                    def title
                    def color
                    def description

                    // currentBuild.result는 최종 빌드 상태를 정확히 반영합니다.
                    if (currentBuild.result == 'SUCCESS' || currentBuild.result == null) {
                        color = 3066993 // 초록색
                        title = "✅ Build Success: ${env.JOB_NAME}"
                        description = "운영 서버 배포에 성공했습니다. #${env.BUILD_NUMBER}"
                    } else {
                        color = 15158332 // 빨간색
                        title = "❌ Build Failed: ${env.JOB_NAME}"
                        description = "운영 서버 배포에 실패했습니다. 확인이 필요합니다. #${env.BUILD_NUMBER}"
                    }

                    // embeds를 사용하는 최신 방식
                    discordSend(
                        webhookURL: "${HOOK_URL}",
                        embeds: [[
                            "title": title,
                            "description": "${description}\n[Jenkins 빌드 로그 보기](${env.BUILD_URL})",
                            "color": color
                        ]]
                    )
                }
            }
            // 정리 작업
            echo 'Pipeline finished. Cleaning up...'
            // docker logout 실패가 빌드 상태에 영향을 주지 않도록 || true 추가
            sh 'docker logout || true'
        }
    }
}