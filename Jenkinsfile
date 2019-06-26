#!/usr/bin/env groovy

pipeline{
    agent {
        label 'slave01'
    }

    parameters {
        string(name: 'GIT_BRANCH', defaultValue: 'master', description: 'The branch where source code will be fetch')
        string(name: 'GIT_SOURCE', defaultValue: 'https://github.com/ohmyohmer/Chat-anonymous', description: 'Source of the source codes.')
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
                    sh '/usr/bin/aws cloudfront create-invalidation --profile group2 --distribution-id E1KAFIBVBSUAER --paths /*'
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
            cleanWs()
        }
        unstable {
            echo 'I am unstable :/'
        }
        failure {
           echo 'Build -> Test -> Deploy Failed!'
        }
        changed {
            echo 'Things were different before...'
        }
    }
}

def notifyProdBuilder() {
    def build_url = "https://jenkins-v2.ibm-kapamilya-devops.com/job/GROUP2/job/Chat-CD/buildWithParameters?token=111cfe3797ebf5ca7c12d7f6ba04258408&parent_build_number=${env.BUILD_NUMBER}"
    def build_message = "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' need to deploy unto the production. Go to ${build_url} to proceed the deployment."
    slackSend (color: '#00FF00', message: build_message)
}

def sendNotif(String status) {

    def colorCode = '#00FF00'
    def subject = "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' - BUILD " + status
    def body = status +": Job ${env.JOB_NAME} [${env.BUILD_NUMBER}]\n More info at: ${env.BUILD_URL}"
    def slackMessage = "${subject} (${env.BUILD_URL})"

    if(status == 'SUCCESS' || status == 'STARTED' || status == 'ENDED') {
        colorCode = '#00FF00'
    } else if(status == 'FAILURE') {
        colorCode  = '#FF0000'
    } else {
        colorCode = '#000000'
    }  
}
