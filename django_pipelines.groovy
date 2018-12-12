#!groovy

// https://plugins.jenkins.io/pipeline-utility-steps
// https://plugins.jenkins.io/ws-cleanup

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
  project_version = null
  project_zip = null
  environment_variables = args.get('project_environment_variables', [])

  withEnv(environment_variables) {
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
      echo "- project_environment_variables: ${environment_variables}"
      echo "-- env.DJANGO_PIPELINES_JENKINS_HOME_VOL: ${env.DJANGO_PIPELINES_JENKINS_HOME_VOL}"
      echo "-- Django Project version: ${project_version}"

      docker.image("mysql:${args.mysql_sidecar.version}").withRun("-e \"MYSQL_ROOT_PASSWORD=${args.mysql_sidecar.root_password}\" -e \"MYSQL_DATABASE=${args.mysql_sidecar.database_name}\"") { db_container ->

        sh 'echo "Waiting Mysql Being ready..." && sleep 10'
        //sh 'while ! mysqladmin ping -h0.0.0.0 --silent; do echo "Waiting mysql being ready" && sleep 1; done'

        // Building the Django artifact
        def docker_config_jenkins_home_vol = args.docker_config_jenkins_home_vol ? args.docker_config_jenkins_home_vol : env.DJANGO_PIPELINES_JENKINS_HOME_VOL
        if (docker_config_jenkins_home_vol)
          error "Jenkins configuration not found - please set docker_config_jenkins_home_vol or DJANGO_PIPELINES_JENKINS_HOME_VOL"

        def docker_extra_options = args.docker_extra_options ? args.docker_extra_options : "-v ${args.docker_config_jenkins_home_vol}:/var/jenkins_home -u root:root"

        docker.image("python:${args.python_version}").inside("$docker_extra_options} --link ${db_container.id}:${args.mysql_sidecar.host_name}") {
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
      }
      
      if (args.node_build_npm) {
        stage('Build Node stuff') {
          // Build Javascript stuff, if needed
          docker.image("node").inside(args.docker_extra_options) {
              unstash 'django_static'

              sh 'npm install -g yarn'
              sh 'cd static; yarn'

              stash includes: 'static/', name: 'django_static_final'
          }
        }
      }
      
      stage('Prepare Artifact') {
        figlet 'DJ - Prepare Artifact'

        cleanWs()
        checkout scm
        
        unstash 'django_static'
        
        zip zipFile: 'project_zip', dir: '.'
        stash includes: 'project_zip', name: 'django_project'
      }
      
      stage('Install Docker Data') {
        figlet 'DJ - Install Docker'
        
        cleanWs()
        unstash 'django_project'
        
        sh 'wget https://github.com/joepreludian/django-jenkins-pipelines/archive/master.zip -O pipeline_temp.zip'
        sh 'unzip pipeline_temp.zip -d .'

        sh "mkdir dist; cd dist; unzip ../project.zip -d .; cp -Rv ../django-jenkins-pipelines-master/django_jenkins_pipeline/* ."
        
        sh "cd dist; cat supervisord.conf"
        sh "cd dist; sed -i \'s/%python_version%/${args.python_version}/g\' Dockerfile"
        sh "cd dist; sed -i \'s/%project_name%/${args.python_django_wsgi}/g\' supervisord.conf"

        zip zipFile: project_zip, dir: 'dist'
        archiveArtifacts artifacts: project_zip, fingerprint: true
        stash includes: project_zip, name: 'django_project_final'
      }

      stage('Post exec') {
        figlet 'DJ - Post Exec'
        
        unstash 'django_project_final'

        if (!args.post_exec) {
          echo 'No post_exec found, skipping'

        } else {
          echo '- post_exec found: executing function injecting variables'
          echo "project_zip: ${project_zip}"
          sh 'ls -lh'
          
          args.post_exec(project_zip)
        }
      }
    }
  }
}

/*
                   figlet 'Django - Generate artifact'
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
