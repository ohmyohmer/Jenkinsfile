#!/usr/bin/env groovy

pipeline{
    agent any

    environment {
        EMAIL_FOR_BUILD_RESULT = 'ibmdevopsjenkins@gmail.com'
    }

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
        stage('Upload') {
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

    emailext body: body
            subject: subject, 
            to: "${env.EMAIL_FOR_BUILD_RESULT}"
            recipientProviders: [[$class: 'DevelopersRecipientProvider']]
}
