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
        HTTPS_PROXY = credentials('proxy')
        HTTP_PROXY = credentials('proxy')
        NO_PROXY = "localhost,127.0.0.0/8,10.0.0.0/8,.suvanet.ch"
        GRADLE_PROPERTIES_TMP = credentials("gradle.properties")
    }
    stages {
        stage('build') {
            steps {
                sh "cp $GRADLE_PROPERTIES_TMP ./gradle.properties"
                sh 'gradle -c settings.mlp.gradle build'
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
                sh 'gradle -c settings.mlp.gradle publish'
            }
        }
    }
}
