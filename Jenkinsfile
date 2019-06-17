#!/usr/bin/env groovy

pipeline{
    agent any
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
