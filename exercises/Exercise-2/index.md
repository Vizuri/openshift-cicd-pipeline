# Exercise 2 - Configure Jenkins Plugins

## Install Jenkins Plugins
A) In *[Jenkins](<http://jenkins-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}> "Jenkins")*, navigate to *Manage Jenkins*.  

   * Scroll down and choose Manage Plugins.
   * Choose the *Available* tab.

B) Install the following Plugins by selecting the checkbox: (*Use the filter on the top right to find the plugins*)
   
   * Anchore Container Image Scanner (under Build Tools)
   * Sonar Quality Gates
   * SonarQube Scanner
   * xUnit
   * Gogs

C) Click on the button *Install without Restart* at the bottom of the page.

## Configure Kubernetes Cloud
The Kubernetes Cloud plugin allows for the running of Kubernetes/OpenShift PODs as Jenkins JNLP Slaves

The OpenShift Jenkins deployment provides a Kubernetes Cloud setup.
It has two out-of-the-box Kubernetes Pod Templates for Jenkins jobs; maven and nodejs.  

We will be using podman to build our containers and pushing them to our container registry.  

An image has already been built for this container.  It can be found at:

<https://docker.io/vizuri/podman:v1.0>

If you would like to see the Dockerfile it can be found here:

<https://github.com/Vizuri/openshift-cicd-podman-jenkins-slave>

This container extends the OpenShift Maven image and just adds the podman binary. 

A) In Jenkins, navigate to the Manage Jenkins 

 * Scroll down to *Configure System*.
 * Scroll down to the Cloud->Kubernetes section.
 * Notice the provided configurations.
 * Add our Podman *Kubernetes Pod Template* by clicking on the *Add Pod Template* button at the bottom of the page and choose *Kubernetes Pod Template*.
 * Enter the following values:
    * Name: `maven-podman` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('maven-podman')" alt="copy-paste" width="20"></a>
    * Labels `maven-podman`
    
    
    <img src="../images/add_container.png" alt="add_container" width="60%">
    

 * Click on the *Add Container* button and choose *Container Template*.
 * Enter the following values:

    * Name: `jnlp`
    * Docker Image: `docker.io/vizuri/podman:v1.0`
    * Working directory: `/tmp`
    * Command to run:   
        >*IMPORTANT: Clear Out The Contents of this Parameter*
    * Arguments to pass to the command: `${computer.jnlpmac} ${computer.name}`
    * Click on *Advanced ...* and make sure the *Run in privileged mode* checkbox is selected
    
     
    <img src="../images/container_arguments.png" alt="container_arguments" width="30%">

 * Click the *Add Volume* button and choose: `Empty Dir Volume`
 * Enter the following values:
    * Mount path: `/var/lib/containers`


    <img src="../images/image10.png" alt="image10" width="60%">

 * Save your changes by clicking on the *Save* button at the bottom.

## Configure Anchore Plugin

A) In Jenkins, navigate to the Manage Jenkins
 
 * Scroll down to *Configure System*.
 * Scroll down to the *Anchore Plugin Mode*.
 * Enter the following values:

    * Engine URL: `http://anchore-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('http://anchore-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}')" alt="copy-paste" width="20"></a>
    * Engine Username: `admin` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('admin')" alt="copy-paste" width="20"></a>
    * Engine Password: `foobar` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('foobar')" alt="copy-paste" width="20"></a>
   
    
    <img src="../images/image3.png" alt="image3" width="60%">

 * Save your changes by clicking on the *Save* button at the bottom.


## Configure SonarQube Plugins

A) In Jenkins, navigate to the Manage Jenkins
 
 * Scroll down to *Configure System*.
 * Scroll down to the *SonarQube* servers section.
 * Click the *Add SonarQube* button.
 * Enter the following values:
    * Name: `sonar` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('sonar')" alt="copy-paste" width="20"></a>
    * Server URL: `http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}')" alt="copy-paste" width="20"></a>

    
    <img src="../images/image9.png" alt="image9" width="60%">

 * Scroll down to the *Quality Gates - SonarQube*.
 * Click the *Add Sonar Instance* button.
 * Enter the following values:

    * Name: `sonar` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('sonar')" alt="copy-paste" width="20"></a>
    * SonarQube Server URL: `http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}')" alt="copy-paste" width="20"></a>
    * SonarQube account login: `admin` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('admin')" alt="copy-paste" width="20"></a>
    * SonarQube account password: `admin`


    <img src="../images/image11.png" alt="image11" width="60%">

 * Click the *Save* button

## Configure SonarQube Jenkins WebHook

* Login into your SonarQube Server.

    <http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}> <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}')" alt="copy-paste" width="20"></a>
    
* Click the Login button and enter:
    * Username: `admin` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('admin')" alt="copy-paste" width="20"></a>
    * Password: `admin`
    
* And press the *Login* button. 

* Click skip this tutorial on the pop-up.

* Click on *Administration* and then choose *WebHooks*.

* Enter the following values:
    * Name: `Jenkins`
    * URL: `http://jenkins-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}/sonarqube-webhook/` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('http://jenkins-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}/sonarqube-webhook/')" alt="copy-paste" width="20"></a>


    <img src="../images/image1.png" alt="image1" width="60%">

* Click the *Save* button
