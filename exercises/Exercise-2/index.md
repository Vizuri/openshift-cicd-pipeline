# Exercise 2 - Configure Jenkins Plugins

## Install Jenkins Plugins
In Jenkins, navigate to Manage Jenkins.  

Choose Manage Plugins.  

Choose the Available tab.

Install the following Plugins:
* Anchore Container Image Scanner
* Sonar Quality Gates Plugin
* SonarQube Scanner for Jenkins
* xUnit plugin
* Gogs

Choose Install without Restart.

## Configure Kubernetes Cloud
The Kubernetes Cloud plugin allows for the running of Kubernetes/OpenShift PODs as Jenkins JNLP Slaves

The OpenShift Jenkins deployment provides a Kubernetes Cloud setup.
It has two out-of-the-box Kubernetes Pod Templates for Jenkins jobs; maven and nodejs.  

We will be using podman to build our containers and pushing them to our container registry.  

A image has alreay been build for this container.  It can be found at:

<docker.io/vizuri/podman:v1.0>

If you would like to see the Dockerfile it can be found here:

<https://github.com/Vizuri/openshift-cicd-podman-jenkins-slave>

This container extends the OpenShift Maven image and just adds the podman binary. 

In Jenkins, navigate to the Manage Jenkins -> Configure System.  Scroll down to the Cloud->Kubernetes section.  

Notice the provided configurations.  

Add our Podman Kubernetes Pod Template.

Click on the Add Pod Template button and choose Kubernetes Pod Template.

Enter the following values:

* Name: maven-podman
* Labels maven-podman


![alt text](../images/image4.png)

Click on the Add Container button and choose Container Template.

Enter the following values:

* Name: jnlp
* Docker Image: docker.io/vizuri/podman:v1.0
* Working directory: /tmp
* Command to run: <Clear Out The Contents of this Parameter>
* Arguments to pass to the command: ${computer.jnlpmac} ${computer.name}


![alt text](../images/Image-100.png)

Click the Add Volume button and choose Empty Dir Volume

Enter the following values:

* Mount path: /var/lib/containers

![alt text](../images/image10.png)

Save your changes.

## Configure Anchore Plugin

In Jenkins, navigate to the Manage Jenkins -> Configure System.
Scroll down to the Anchore Plugin Mode

Enter the following values:

* Engine URL: <http://anchore-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}>
* Engine Username: admin
* Engine Password: foobar


![alt text](../images/image3.png)

Click Save


## Configure SonarQube Plugins
In Jenkins, navigate to the Manage Jenkins -> Configure System.   

Scroll down to the SonarQube servers section. 

Click the Add SonarQube button.

Enter the following values:
* Name: sonar
* Server URL: <http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}>


![alt text](../images/image9.png)

Scroll down to the Quality Gates SonarQube

Click the Add Sonar Instance button

Enter the following values:

* Name: sonar
* SonarQube Server URL: <http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}>
* SonarQube account login: admin
* SonarQube account password: admin

![alt text](../images/image11.png)

Click the Save button

## Configure SonarQube Jenkins WebHook.
Login into your SonarQube Server.

<http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}>

Click the Login button and enter

* Username: admin
* Password: admin

And press the Login button. 

Click skip this tutorial on the pop-up.

Click on Administration and then choose WebHooks.

Enter the following values:
* Name: Jenkins
* URL: <http://jenkins-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}/sonarqube-webhook/>

![alt text](../images/image1.png)

Click Save
