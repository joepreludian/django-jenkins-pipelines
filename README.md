# Django Jenkins Pipeline

A dockerized pipeline for build Django applications into Docker-in-docker Jenkins trying to use best development practices;
In a nutshell Django Jenkins pipelines aims to turn the Django web framework, a well consolidated web framework, even more productive, automating their deployment processes into a straightforward, clean build and deployment.
Also this pipeline aims to be in conformance with Cloud application best practices, being easily handled with infrastructure automation tools, like Chef/Puppet/Ansible, and even Docker!


# Jenkins Docker in Docker 

Jenkins Docker in docker is an effort to make a Jenkins running into a dockerized environment. Also I've created a Jenkins Installer that can be found here: [](https://github.com/joepreludian/jenkins-docker-in-docker).

# What does it needed anyway?

The proposal of Django Jenkins Pipelines is to create a standardized ecossystem trying to use Jenkins best practices in order to build Django applications efficiently through Docker containers. It can easily be used with Jenkins, public (or not), aimed for small to medium projects.
I decided to use the core spirit of Django, when we talk about productivity bootstrapping their applications, in use on CI/CD Pipeline. I reallize that it's pretty much difficult to put a Django project to run using a VPS or not depending of such kind of tools like Heroku and or TravisCI, for example.

They used to be free only for Opensource Projects, which seems marvelous, but when we need to create a more private projects this don't come handy. My Idea is to have a good pipeline that could be used by anyone who wants to build their projects into a stardardized way, using the edge technologies/methods in order to get your application running in a production environment.
That's should be enough to teach people how the Continuous Integration/ Continuous Delivery means and turn this technologies being acessible to SOHO development. I hope to attend the spectations. =)

# Features
The features of this pipelines covers the following points:

* Install python dependencies;
* Running database migrations using a Mysql Sidecar;
* Collecting static files;
* Run yarn in order to install javascript dependencies;
* Generate an Artifact; (A zip file with the project ready for being installed)
* Inject Docker bootstrap - It's some docker configuration that could being injected into code in order to build a fully functional docker image with supervisor, nginx and gunicorn;
* Creating a post exec handler that you will being able to have the artifact name and the docker image name injected into it: useful to exec the deployment phase. Here you can trigger from a chef recipe to a fabric/ssh handling, injecting the artifact inside the final server;

# Running the pipeline


## Django projects

In order to get this pipeline running your Django project ought to be aligned with some conditions;
* Use Pipenv as default package manager: Pipenv is the cut edge python dependency management system. I used to say that Pipenv will be a frontend for Pip. Read more at [](https://pipenv.readthedocs.io/en/latest/);
  * Pipenv also use .env files in order to use environment variables (good practice according [12Factor](https://12factor.net/))
* Have a mysql support;
* Have a static folder defined. (It will use /static as a base folder)

A tool that will make something like a "preflight check" will be on course in order to turn easy to adapt your Django project for this pipeline without taking too much customizations.

## Basic running usage

You ought to create, on your project, the following `Jenkinsfile`;
Also I've created a project used for testing the pipeline. It's a Django with only the needed configuration for this pipeline. You can reach it on [](https://github.com/joepreludian/django-template/blob/master/Jenkinsfile);

```
def django_pipelines = fileLoader.fromGit('django_pipelines', 'https://github.com/joepreludian/django-jenkins-pipelines.git', 'master', null, '')

def post_exec_handler = { project_zip, docker_image ->
  figlet 'Installing'
  echo 'Running post scripts'
  echo docker_image
  sh "mkdir project_final && unzip ${project_zip} -d project_final"
  sh 'ls -lh project_final/'
}

django_pipelines.buildProject(
  project_name: 'django_template',  //The name of project (an artifact and a Docker image will be generated with this name)
  python_django_main_module: 'djproject',  //The name of the main module of your project (where is located the settings file)
  python_version: '3.7',  //The version of python that is going to be used
 
  master_as_release_branch: true,  // Treat the master branch of your project as a release branch, otherwise only use the branches named by "release/.*" as a release branches (A release branch will do the end tasks [like publishing or execute your post_exec_handler])

  node_yarn_install_static: true, // If true, it will run a yarn install after django run the collectstatic on the static folder;
  project_docker_inject: true,  // If true, it will inject a default Dockerfile with default configuration files for your project that includes simple nginx error pages, a configuration for using supervisor, nginx and gunicorn on a same container;
  project_docker_create_image: true,  //Mark this as true and the pipeline will try to deploy your project;

  // Specific configurations for using mysql sidecar; On every build we going to raise an ephemeral mysql instance attached to the current django pipeline in order to serve your project. At the end the instance will be destroyed.
  mysql_sidecar: [
    root_password: '302010',
    host_name: 'DB',
    database_name: 'djproject',
    version: '5.7'
  ],

  project_environment_variables: [
    'DJANGO_ENVIRONMENT=dev',
    'DB_HOST=DB',
    'DB_NAME=djproject',
    'DB_NAME_TEST=djproject_test',
    'DB_PASSWORD=302010'
  ],

  // This is a closure that will exec post_exec_handler, declared above; It will inject both the artifact name and the docker_image, if provided on project_docker_create_image setting.
  post_exec: post_exec_handler 
)
```

# Roadmap

This is an alpha project. Script variations should happen time to time. I strongly advise that keep a track on this repo. Please test it! Notes, bugfixes or enhancements, please let me know on the Issues section.
If you live in Curitiba, PR, let's drink a coffee somewhere. =)

As a roadmap I will try to:

* Develop build tests;
* Create more use cases in order to turn this pipeline as a very customizable using the same approach as Django: try to do magic with the Defaults; especialize only if needed, keeping the process clean;
* Create a good wiki with a bunch of examples.

I hope that this project helps you to boost your productivity.
