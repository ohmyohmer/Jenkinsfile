#!/usr/bin/env groovy

pipeline{
    agent {
        label 'slave01'
    }

    parameters {
        string(name: 'GIT_BRANCH', defaultValue: 'master', description: 'The branch where source code will be fetch')
        string(name: 'GIT_SOURCE', defaultValue: 'https://github.com/ohmyohmer/Chat-anonymous', description: 'Source of the source codes.')
        string(name: 'BUILD_APPROVER_EMAIL', defaultValue: 'escuetamichael@gmail.com', description: 'Build Approver Email')
        string(name: 'BUILD_APPROVER_NAME', defaultValue: 'Michael Escueta', description: 'Build Approver Name')
    }

    options {
        timestamps()
    }

    stages {
        stage('Build Started') {
            steps {
                echo "Build started..."
            }
        }
        stage('Source') {
            steps {
                git url: "${params.GIT_SOURCE}", branch: "${params.GIT_BRANCH}"
            }
        }
        stage('Install Dependencies') {
            steps {
                sh 'npm install'
            }
        }
        stage('Test Started') {
            steps {
                echo "Test started..."
            }
        }
        stage('Test - Run the linter (tslint)') {
            steps {
                sh 'npm run lint'
            }
        }
        stage('Code Analysis (SonarQube)') {
            steps {
                script {
                    scannerHome = tool 'SONARQUBE_SCANNER'
                }
                withCredentials([string(credentialsId: 'GROUP2_SONARQUBE_TOKEN', variable: 'GROUP2_SONARQUBE_TOKEN')]) {
                    withSonarQubeEnv('SONARQUBE_SERVER') {
                        sh "${scannerHome}/bin/sonar-scanner -Dsonar.login=$GROUP2_SONARQUBE_TOKEN -Dsonar.projectVersion=$BUILD_NUMBER"
                    }
                }
                timeout(time: 2, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                } 
            }
        }
        stage('Test Ended') {
            steps {
                echo "...test ended."
            }
        }
        stage('Build') {
            steps {
                sh 'npm run build:prod:en'
            }
        }
        stage('Build Ended') {
            steps {
                echo "...build ended."
            }
        }
        stage('Upload Artifact (JFrog)') {
            steps {
                withCredentials([string(credentialsId: 'ARTIFACTORY_USERNAME_', variable: 'ARTIFACTORY_USERNAME_'), string(credentialsId: 'ARTIFACTORY_PASSWORD_', variable: 'ARTIFACTORY_PASSWORD_')]) {
                    sh '''
                        ARTIFACT_NAME=artifact.group2.$BUILD_NUMBER.tar.bz2
                        touch dist/kapamilya-chat/version.txt
                        echo "Build #:$BUILD_NUMBER" >> dist/kapamilya-chat/version.txt
                        tar -cj dist/* > $ARTIFACT_NAME
                        curl -u$ARTIFACTORY_USERNAME_:$ARTIFACTORY_PASSWORD_ -T $ARTIFACT_NAME "https://jfrog-v2.ibm-kapamilya-devops.com/artifactory/group2/$ARTIFACT_NAME"
                    '''
                }
            }
        }  
        stage('Download Artifact (JFrog)') {
            steps {
                withCredentials([string(credentialsId: 'ARTIFACTORY_USERNAME_', variable: 'ARTIFACTORY_USERNAME_'), string(credentialsId: 'ARTIFACTORY_PASSWORD_', variable: 'ARTIFACTORY_PASSWORD_')]) {
                    sh '''
                        curl -O "https://jfrog-v2.ibm-kapamilya-devops.com/artifactory/group2/artifact.group2.$BUILD_NUMBER.tar.bz2"
                        tar -xjf artifact.group2.$BUILD_NUMBER.tar.bz2
                    '''
                }
            }
        }
        stage('Staging Upload (Amazon S3)') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'AWS_STAGING_CREDENTIALS']]) {
                    s3Upload bucket: 'group2-staging.ibm-kapamilya-devops.com', file: "kapamilya-chat", workingDir: 'dist', acl: 'PublicRead'
                    sh '''
                        /usr/bin/aws cloudfront create-invalidation --distribution-id E1KAFIBVBSUAER --paths '/*'
                    '''
                    }
            }
        }
    }
    post {
        always {
            echo 'Always on run :D'
        }
        success {
            echo 'Build -> Test -> Deploy Success!'
            sendProdApproval()
            buildStatusNotif("${currentBuild.currentResult}")
            cleanWs()
        }
        unstable {
            echo 'I am unstable :/'
        }
        failure {
           echo 'Build -> Test -> Deploy Failed!'
           buildStatusNotif("${currentBuild.currentResult}")
        }
        changed {
            echo 'Things were different before...'
        }
    }
}

def buildStatusNotif(String status) {
    def this_message = "Project Chat - BUILD " + status + ". For more info: '${env.BUILD_URL}'"
    slackSend (color: '#00FF00', message: this_message)
}

def sendProdApproval() {
    def redirectTo = "https://jenkins-v2.ibm-kapamilya-devops.com/job/GROUP2/job/Chat-CD/${env.BUILD_NUMBER}"
    def build_url = "https://jenkins-v2.ibm-kapamilya-devops.com/job/GROUP2/job/Chat-CD/buildWithParameters"
    def build_token = "111cfe3797ebf5ca7c12d7f6ba04258408"

    def form = "Hi ${BUILD_APPROVER_NAME}, <p>Job <strong>Project Chat</strong> is ready for deployment.</p>Awaiting for your approval, kindly click the button below to proceed and disregard this email if not so.<p></p><form method='post' action='${build_url}?token=${build_token}&parent_build_number=${env.BUILD_NUMBER}'><input name='crumb' type='hidden' value='68f6099a9b8b4ac0194d03ac6cfae817'><input name='json' type='hidden' value=\"{'parameter': {'name': 'TRIGGERED_FROM_BUILD', 'runId': '${JOB_NAME}#${BUILD_NUMBER}'}, 'statusCode': '303', 'redirectTo': '${redirectTo}'}\" /><input name='Submit' type='submit' value='Deploy' class='submit-button primary' /></form>"
    def this_subject = "Project Chat for Production Deployment"
    def this_body = form
    
    emailext mimeType: 'text/html', body: this_body, subject: this_subject, to: '${BUILD_APPROVER_EMAIL}'
}
