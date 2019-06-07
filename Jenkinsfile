pipeline{
    agent any
    stages {
        stage('Source') {
            steps {
                git branch: 'develop', url: 'https://github.com/ohmyohmer/cashew'
            }
        }
    }
}