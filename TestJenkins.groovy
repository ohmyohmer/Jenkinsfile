#!/usr/bin/env groovy

pipeline{
    agent any
    
    stages {
        stage('Source') {
            steps {
                git branch: 'develop', url: 'https://github.com/ohmyohmer/cashew'
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
        stage('Upload Artifact') {
            steps {
                withCredentials([string(credentialsId: 'ARTIFACTORY_USERNAME_', variable: 'ARTIFACTORY_USERNAME_'), string(credentialsId: 'ARTIFACTORY_PASSWORD_', variable: 'ARTIFACTORY_PASSWORD_')]) {
                    sh '''
                        ARTIFACT_NAME=artifact.group2.$BUILD_NUMBER.tar.bz2
                        tar -cj dist/* > $ARTIFACT_NAME
                        curl -u$ARTIFACTORY_USERNAME_:$ARTIFACTORY_PASSWORD_ -T $ARTIFACT_NAME "https://jfrog.ibm-kapamilya-devops.com/artifactory/generic-local/$ARTIFACT_NAME"
                    '''
                }
            }
        }
        stage('S3 Upload') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'AWS_Id', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                }
            }
        }
    }
    post {
        always {
          sendNotif("${currentBuild.currentResult}");
        }
    }
}
def sendNotif(String status) {

    def colorCode = '#00FF00'
    def subject = "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' - BUILD " + status
    def body = status +": Job ${env.JOB_NAME} [${env.BUILD_NUMBER}]\n More info at: ${env.BUILD_URL}"
    def slackMessage = "${subject} (${env.BUILD_URL})"

    if(status == 'SUCCESS') {
        colorCode = '#00FF00'
    } else if(status == 'FAILURE') {
        colorCode  = '#FF0000'
    } else {
        colorCode = '#000000'
    }

    slackSend (color: colorCode, message: slackMessage)
}