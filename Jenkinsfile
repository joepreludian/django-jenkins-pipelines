#!groovy

set wsgi_container = 'TESTE'

pipeline {
    agent none

    options {
        buildDiscarder(logRotator(numToKeepStr: '4', artifactNumToKeepStr: '4'))
    }

    triggers {
        pollSCM('H/30 * * * *')
    }

    stages {
        stage('Build Project') {
            agent {
                docker { 
                    image 'python:3.6'
                    args "-v wi-vol-jenkins:/var/jenkins_home"
                }
            }
            steps {
                checkout scm
                //sh 'mvn package'
                //archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                //stash includes: '**/target/*.jar', name: 'app'
                echo "PRINTING MESSAGE: {wsgi_container}"
            }
        }
/*        
        stage('Deploy to PCF') {
            agent any
            steps {
                unstash 'app'
                echo 'Pushing project to cloudfoundry'
                
                pushToCloudFoundry(
                  target: 'https://api.sys.pcf-aws.com',
                  organization: 'wipro-poc',
                  cloudSpace: 'training',
                  credentialsId: 'pcf_jon_user'
                )
            }
        }

        stage('Test PCF Project') {
            agent {
                docker {
                    image 'python:3.6'
                    args '-v wi-vol-jenkins:/var/jenkins_home'
                }
            }
            steps {
                sh 'pip install pipenv && pipenv install'
                sh 'pipenv run python test_suite.py'
            }
            post {
                always {
                    junit '**/test-reports/*.xml'
                }
            }
        }
*/
    }
}

