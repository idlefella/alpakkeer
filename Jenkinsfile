@Library('openshift-jenkins-lib@master')
def lib = new Libs().importLibraries()

pipeline {
    agent {
        label 'aio-java11'
    }
    environment {
        PROJECT_NAME_SHORT='mlp'
        REPO_NAME="alpakkeer"
        SONARQUBE_HOST="http://sonarqubeintg.suvanet.ch:9000"
        SONARQUBE_TOKEN=credentials('sonarqube-intg-token')
    }
    stages {
        stage('build') {
            steps {
                dir("core") {
                    sh 'gradle -b build_mlp.gradle build'
                }
            }
        }
        stage('sonarqube') {
            when {
                expression {
                    return false
                }
            }
            steps {
                echo "TODO"
            }
        }
        stage('deploy') {
            steps {
                dir("core") {
                    sh 'gradle -b build_mlp.gradle publish'
                }
            }
        }
    }
}
