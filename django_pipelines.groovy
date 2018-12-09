#!groovy

// https://plugins.jenkins.io/pipeline-utility-steps
// https://plugins.jenkins.io/ws-cleanup

/*
project_name = 'my_test_project'
project_python_version = '3.6'
wsgi_container = 'test_project'
docker_image_name = 'preludian/temp_project'
docker_image_cleanup_old_builds = true
docker_listen_port = 8001

docker_args = '-v prl-vol-jenkins:/var/jenkins_home -u root:root'
*/

project_version = null
project_zip = null

def buildProject(args) {
    
    /*
        Django Build Pipeline - A Simple Jenkins pipeline to build Django Projects
        ---------------------
        arguments: 
            * docker_extra_options - string - Put extra options inside any projects
            
            * python_django_wsgi - string - required - Name of django wsgi container
            * python_django_main_module - required - Name of django main module

            * python_version - required - string [2.7|3.6]
            * build_npm - required - boolean
    */

    // @todo Add container linking for mysql and postgresql 
    // @todo Inject environment variables 
    
    node {
        cleanWs()
        checkout scm

        // Fetching the project version
        project_version = sh returnStdout: true, script: 'cat VERSION || echo "latest"'
        project_version = project_version.trim()

        // Composition of the project Artifact
        project_zip = "${args.project_name}-${project_version}-b${env.BUILD_NUMBER}.zip"
        
        figlet "Django Pipelines"

        echo "Project ZIP name: ${project_zip}"

        figlet "Django Pipelines - args"
        
        echo "- docker_extra_options: ${args.docker_extra_options}"
        echo "- python_django_wsgi: ${args.python_django_wsgi}"
        echo "- python_django_main_module: ${args.python_django_main_module}"
        echo "- python_version: ${args.python_version}"
        echo "-- Django Project version: ${project_version}"


        // Building the Django artifact
        docker.image("python:${args.python_version}").inside(args.docker_extra_options) {
            stage('Install dependencies') {
                figlet "Project version ${project_version}"
                figlet 'Django - Install dependencies'

                echo "PROJECT NAME: ${args.python_django_wsgi} - MAIN MODULE: ${args.python_django_main_module}"
                sh 'pip install --upgrade pipenv && pipenv install --system --deploy'
            }

            stage('Run migrations') {
                figlet 'Django - Run migrations'
                sh 'python manage.py migrate'
            }

            stage('Collect Static') {
                figlet 'Django - Collect Static'

                sh 'python manage.py collectstatic --noinput'
                stash includes: 'static/', name: 'django_static'
            }
        }
        
        if (args.node_build_npm) {
          stage('Build Node stuff') {
              // Build Javascript stuff, if needed
              if (args.node_build_npm) {
                  docker.image("node").inside(args.docker_extra_options) {
                      unstash 'django_static'

                      sh 'npm install -g yarn'
                      sh 'cd static; yarn'

                      stash includes: 'static/', name: 'django_static_final'
                  }
              }
          }
        }

    }
}

/*
def runPipeline(params) {
    pipeline {

        agent none

        options {
            buildDiscarder(
                logRotator(
                    numToKeepStr: '4', 
                    artifactNumToKeepStr: '4'
                )
            )
        }

        triggers {
            pollSCM('H/30 * * * *')
        }

        environment {
            DJANGO_PRODUCTION = '1'
        }

        stages {

            stage('Build Django') {
                agent any
                steps {
                    projectPipelineStart docker_extra_options: docker_args, wsgi: 'test_project', main_module: 'Main Module'
                }
            }

            stage('Build project - Install JS Libraries') {
                agent {
                    docker { 
                        image 'node'
                        args docker_args
                    }
                }
                steps {
                    figlet 'JS - Install Libraries'
                    unstash 'django_static'

                    sh 'npm install -g yarn'
                    sh 'cd static; yarn'

                    stash includes: 'static/', name: 'django_static_final'
                }
            }

            stage('Generate Artifact') {
                agent any
                steps {
                    cleanWs()
                    checkout scm

                    unstash 'django_static_final'

                    figlet 'Django - Generate artifact'
                    zip zipFile: 'project.zip', dir: '.'

                    stash includes: 'project.zip', name: 'django_project'
                    
                }
            }

            stage ('Create container') {
                agent any
                when {
                    branch 'master'
                }
                steps {
                    cleanWs()
                    
                    unstash 'django_project'
                    
                    echo 'Deployment container'
                                        
                    sh 'wget https://github.com/joepreludian/django-jenkins-pipelines/archive/master.zip -O pipeline_temp.zip'
                    sh 'unzip pipeline_temp.zip -d .'

                    sh "mkdir dist; cd dist; unzip ../project.zip -d .; cp -Rv ../django-jenkins-pipelines-master/django_jenkins_pipeline/* ."
                    
                    sh "cd dist; cat supervisord.conf"
                    sh "cd dist; sed -i \'s/%python_version%/${project_python_version}/g\' Dockerfile"
                    sh "cd dist; sed -i \'s/%project_name%/${wsgi_container}/g\' supervisord.conf"

                    zip zipFile: project_zip, dir: 'dist'
                    archiveArtifacts artifacts: project_zip, fingerprint: true

                    sh "cd dist; docker build -t ${docker_image_name}:${project_version} -t ${docker_image_name}:latest ."  // @TODO Refactor docker image to test the latest 
                }
            }

            stage('Deploy docker container') {
                agent any
                when {
                    branch 'master'
                }
                steps {

                    echo "Using Docker image: ${docker_image_name}:${project_version}"
                    echo "Removing running instances"
                
                    // @TODO Refactor this to handle it efficiently

                    params['buildClosure']()

                    sh "docker stop ${project_name} || true"
                    sh "docker rm ${project_name} || true"

                    sh "docker run -it -d --name ${project_name} -p ${docker_listen_port}:80 ${docker_image_name}:${project_version}"

                }
            }

        }
    }
}
*/

return this
