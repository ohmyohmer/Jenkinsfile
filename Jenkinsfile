#!/usr/bin/env groovy

pipeline{
    agent {
        label 'slave01'
    }

    parameters {
        string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'The branch where source code will be fetch')
        string(name: 'GIT_SOURCE', defaultValue: 'https://github.com/ohmyohmer/cashew', description: 'Source of the source codes.')
    }
    stages {
        stage('Source') {
            steps {
                git branch: '${params.GIT_BRANCH}', url: '${params.GIT_SOURCE}'
            }
        }
        stage('Install Dependencies') {
            steps {
                sh 'npm install'
            }
        }
        stage('Test - Run the linter (tslint)') {
            steps {
                sh 'npm run lint'
            }
        }
        stage('Sonarqube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'SONARQUBE_APIKEY_', variable: 'SONARQUBE_APIKEY_')]) {
                    withSonarQubeEnv('SonarqubeServer_GROUP2') {
                        sh '/usr/local/bin/sonar-scanner -Dsonar.login=$SONARQUBE_APIKEY_ -Dsonar.projectVersion=$BUILD_NUMBER'
                    }
                }
                timeout(time: 2, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                } 
            }
        }
        stage('Build') {
            steps {
                sh 'npm run build:prod:en'
            }
        }
        stage('Upload Artifact') {
            steps {
                withCredentials([string(credentialsId: 'ARTIFACTORY_USERNAME_', variable: 'ARTIFACTORY_USERNAME_'), string(credentialsId: 'ARTIFACTORY_PASSWORD_', variable: 'ARTIFACTORY_PASSWORD_')]) {
                    sh '''
                        ARTIFACT_NAME=artifact.group2.$BUILD_NUMBER.tar.bz2
                        touch dist/cashew/version.txt
                        echo "Build #:$BUILD_NUMBER" >> dist/cashew/version.txt
                        tar -cj dist/* > $ARTIFACT_NAME
                        curl -u$ARTIFACTORY_USERNAME_:$ARTIFACTORY_PASSWORD_ -T $ARTIFACT_NAME "https://jfrog.ibm-kapamilya-devops.com/artifactory/generic-local/$ARTIFACT_NAME"
                    '''
                }
            }
        }
        stage('Download Artifact') {
            steps {
                withCredentials([string(credentialsId: 'ARTIFACTORY_USERNAME_', variable: 'ARTIFACTORY_USERNAME_'), string(credentialsId: 'ARTIFACTORY_PASSWORD_', variable: 'ARTIFACTORY_PASSWORD_')]) {
                    sh '''
                        curl -O "https://jfrog.ibm-kapamilya-devops.com/artifactory/generic-local/artifact.group2.$BUILD_NUMBER.tar.bz2"
                        tar -xjf artifact.group2.$BUILD_NUMBER.tar.bz2
                    '''
                }
            }
        }
        stage('S3 Upload') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'AWS_Id', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    s3Upload bucket: 'images-boyband.abs-cbn.com/jenkins', file: "cashew", workingDir: 'dist'
                    sh 'ls -l'
                }
            }
        }
    }
    post {
        always {
            echo 'Always on run :D'
        }
        success {
            sendNotif("${currentBuild.currentResult}");
        }
        unstable {
            echo 'I am unstable :/'
        }
        failure {
            sendNotif("${currentBuild.currentResult}");
        }
        changed {
            echo 'Things were different before...'
        }
    }
}

def sendNotif(String status) {

    def colorCode = '#00FF00'
    def email_subject = "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' - BUILD " + status
    def body = status +": Job ${env.JOB_NAME} [${env.BUILD_NUMBER}]\n More info at: ${env.BUILD_URL}"
    def slackMessage = "${email_subject} (${env.BUILD_URL})"

    if(status == 'SUCCESS') {
        colorCode = '#00FF00'
    } else if(status == 'FAILURE') {
        colorCode  = '#FF0000'
    } else {
        colorCode = '#000000'
    }

    slackSend (color: colorCode, message: slackMessage)
}
