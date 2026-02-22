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
                // groovy 스크립트에서는 변수 치환시 ''가 아니고 "" 사용 필요
                git branch: "${env.BRANCH_NAME}",
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
                script {
                    // - main  -> prod
                    // - 그 외 -> qa
                    env.DEPLOY_ENV = (env.BRANCH_NAME == 'main') ? 'prod' : 'qa'

                    echo "### DEPLOY_ENV: ${env.DEPLOY_ENV}"

                    // 환경별 Credentials ID 분기
                    def WAS_HOST_CRED        = (env.DEPLOY_ENV == 'prod') ? 'PROD_WAS_HOST' : 'QA_WAS_HOST'
                    def USERNAME_CRED        = (env.DEPLOY_ENV == 'prod') ? 'PROD_USERNAME' : 'QA_USERNAME'
                    def SSH_PORT_CRED        = (env.DEPLOY_ENV == 'prod') ? 'PROD_WAS_SSH_PORT' : 'QA_WAS_SSH_PORT'

                    def ENV_PROPERTIES_CRED  = (env.DEPLOY_ENV == 'prod') ? 'PROD_ENV_PROPERTIES' : 'QA_ENV_PROPERTIES'
                    //def GOOGLE_JSON_CRED     = (env.DEPLOY_ENV == 'prod') ? 'PROD_GOOGLE_JSON_BASE64' : 'QA_GOOGLE_JSON_BASE64'
                    //def FIREBASE_JSON_CRED   = (env.DEPLOY_ENV == 'prod') ? 'PROD_FIREBASE_KEY_JSON_BASE64' : 'QA_FIREBASE_KEY_JSON_BASE64'

                    // DockerHub 정보 prod/qa 분리
                    def DOCKERHUB_CRED       = (env.DEPLOY_ENV == 'prod') ? 'PROD_DOCKERHUB_CREDENTIALS' : 'QA_DOCKERHUB_CREDENTIALS'
                    def IMAGE_NAME_CRED      = (env.DEPLOY_ENV == 'prod') ? 'PROD_IMAGE_NAME' : 'QA_IMAGE_NAME'

                    // SSH 키 환경별 분리
                    def SSH_KEY_CRED         = (env.DEPLOY_ENV == 'prod') ? 'PROD_PRIVATE_KEY' : 'QA_PRIVATE_KEY'

                    withCredentials([
                        // Docker & Server Info
                        string(credentialsId: IMAGE_NAME_CRED, variable: 'IMAGE_NAME'),
                        string(credentialsId: WAS_HOST_CRED, variable: 'WAS_HOST'),
                        string(credentialsId: USERNAME_CRED, variable: 'WAS_USERNAME'),
                        string(credentialsId: SSH_PORT_CRED, variable: 'WAS_SSH_PORT'),

                        // Application Config Files
                        file(credentialsId: ENV_PROPERTIES_CRED, variable: 'ENV_PROPERTIES_FILE_PATH'),
                        string(credentialsId: 'PROD_GOOGLE_JSON_BASE64', variable: 'GOOGLE_JSON_B64'),
                        string(credentialsId: 'PROD_FIREBASE_KEY_JSON_B64', variable: 'FIREBASE_KEY_B64')
                        //string(credentialsId: GOOGLE_JSON_CRED, variable: 'GOOGLE_JSON_B64'),
                        //string(credentialsId: FIREBASE_JSON_CRED, variable: 'FIREBASE_KEY_B64')
                    ]) {
                        // DockerHub 계정/토큰은 usernamePassword로 관리
                        withCredentials([usernamePassword(
                            credentialsId: DOCKERHUB_CRED,
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )]) {

                            // sshagent를 withCredentials 리스트가 아닌, 중첩된 래퍼(wrapper)로 올바르게 사용
                            sshagent(credentials: [SSH_KEY_CRED]) {
                                // DOCKER_USER/PASS 변수와 SSH 키를 모두 사용 가능
                                // 3. 운영/QA 환경 설정 파일 생성
                                stage('Generate Config Files') {
                                    sh '''
                                        mkdir -p src/main/resources
                                        # ENV_PROPERTIES_FILE_PATH 변수에는 임시 파일의 경로가 담겨있음
                                        rm -f src/main/resources/env.properties
                                        cp "${ENV_PROPERTIES_FILE_PATH}" src/main/resources/env.properties
                                        echo "${GOOGLE_JSON_B64}" | base64 -d > src/main/resources/google.json
                                        echo "${FIREBASE_KEY_B64}" | base64 -d > src/main/resources/whiplash-firebase-key.json
                                    '''
                                }

                                // 4. Gradle 빌드 -> Dockerfile에서 이미 빌드하므로 주석 처리
                                //stage('Build') {
                                //    sh 'chmod +x ./gradlew'
                                //    sh './gradlew bootJar --no-daemon'
                                //}

                                // 5. Docker 이미지 빌드 및 푸시
                                stage('Build and Push Docker Image') {
                                    sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                                    sh "docker build -t ${IMAGE_NAME}:${env.SHORT_SHA} ."
                                    sh "docker push ${IMAGE_NAME}:${env.SHORT_SHA}"
                                }

                                // 6. 운영/QA 서버에 무중단 배포 실행
                                stage("Deploy Blue/Green to ${env.DEPLOY_ENV.toUpperCase()}") {
                                    sh """
                                        ssh -p ${WAS_SSH_PORT} -o StrictHostKeyChecking=no ${WAS_USERNAME}@${WAS_HOST} 'cd /opt/app/scripts && ./deploy.sh "${env.SHORT_SHA}" "${IMAGE_NAME}" "${DOCKER_USER}" "${DOCKER_PASS}" "${env.DEPLOY_ENV}"'
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
        success {
            withCredentials([string(credentialsId: 'DISCORD_WEBHOOK_URL', variable: 'HOOK_URL')]) {
                script {
                    def envLabel = (env.DEPLOY_ENV == 'prod') ? "운영" : "QA"

                    def titleMsg = (env.DEPLOY_ENV == 'prod') ?
                            "✅ 눈 떠! 운영 API 서버 배포 성공 (Job : ${env.JOB_NAME})" :
                            "🧪 눈 떠! QA API 서버 배포 성공 (Job : ${env.JOB_NAME})"

                    def descMsg = (env.DEPLOY_ENV == 'prod') ?
                            "운영 API 서버 배포에 성공했습니다. #${env.BUILD_NUMBER}" :
                            "QA API 서버 배포에 성공했습니다. #${env.BUILD_NUMBER}"

                    discordSend(
                        webhookURL: "${HOOK_URL}",
                        title: titleMsg,
                        description: descMsg,
                        link: env.BUILD_URL,
                        result: currentBuild.currentResult,
                        footer: "Environment: ${envLabel} | Branch: ${env.BRANCH_NAME} | Commit: ${env.SHORT_SHA}"
                    )
                }
            }
        }
        failure {
            withCredentials([string(credentialsId: 'DISCORD_WEBHOOK_URL', variable: 'HOOK_URL')]) {
                script {
                    def envLabel = (env.DEPLOY_ENV == 'prod') ? "운영" : "QA"

                    def titleMsg = (env.DEPLOY_ENV == 'prod') ?
                            "❌ 눈 떠! 운영 API 서버 배포 실패 (Job : ${env.JOB_NAME})" :
                            "🚨 눈 떠! QA API 서버 배포 실패 (Job : ${env.JOB_NAME})"

                    def descMsg = (env.DEPLOY_ENV == 'prod') ?
                            "운영 API 서버 배포에 실패했습니다. 확인이 필요합니다. #${env.BUILD_NUMBER}" :
                            "QA API 서버 배포에 실패했습니다. 확인이 필요합니다. #${env.BUILD_NUMBER}"

                    discordSend(
                        webhookURL: "${HOOK_URL}",
                        title: titleMsg,
                        description: descMsg,
                        link: env.BUILD_URL,
                        result: currentBuild.currentResult,
                        footer: "Environment: ${envLabel} | Branch: ${env.BRANCH_NAME} | Commit: ${env.SHORT_SHA}"
                    )
                }
            }
        }
        always {
            // 정리작업
            echo 'Pipeline finished. Cleaning up...'
            // docker logout 실패가 빌드 상태에 영향을 주지 않도록 || true 추가
            sh 'docker logout || true'
        }
    }
}