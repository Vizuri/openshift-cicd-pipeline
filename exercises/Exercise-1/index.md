# Exercise 1 - Explore Lab Environment

## Explore OpenShift Environment

A) Navigate to the OpenShift Console: <{{ ocp_console_url }}> <a href="#0"><a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('{{ ocp_console_url }}')" alt="copy-paste" width="20"></a>

   * Username: `student-{{ student_number }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('student-{{ student_number }}')" alt="copy-paste" width="20"></a>
   * Password: `{{ student_pwd }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('{{ student_pwd }}')" alt="copy-paste" width="20"></a>
 
     >Note: Click on the **Advanced** button and click on the link: **Proceed to ocpws.kee.vizuri.com (unsafe)**
    
B) You have four projects pre-created for you (*Under My Projects on the right*)

* CICD - Tools needed for CICD Pipeline
    * anchore - Container Scanning Service
    * anchoredb - Database for Anchore Container Scanning Service
    * jenkins - Jenkins
    * nexus - Nexus artifact reposigory
    * sonarqube - Code Quality Service
    * sonardb - Database for Code Quality Service
    
* Customer Development - Development Project
    * customerdb - Mongo Database for Development Service
    
* Customer Test - Test Project
    * customerdb - Mongo Database for Test Service
    
* Customer Prod - Production Project
    * customerdb - Mongo Database for Production Service


## Explore Jenkins

A) Confirm you can log into your jenkins console: <http://jenkins-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}> <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('http://jenkins-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}')" alt="copy-paste" width="20"></a>

   * Username: `student-{{ student_number }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('student-{{ student_number }}')" alt="copy-paste" width="20"></a>
   * Password: `{{ student_pwd }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('{{ student_pwd }}')" alt="copy-paste" width="20"></a>

     >Note: Click on the **Allow selected permissions** button the first time: <img src="../images/jenkins_accept.png" alt="jenkins_accept" width="500"> 

B) Should see the message: 

<img src="../images/jenkins_welcome.png" alt="jenkins_welcome" width="200">

## Explore Nexus

A) Confirm you can navigate to nexus: <http://nexus-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}> <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('http://nexus-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}')" alt="copy-paste" width="20"></a>

B) Should see the message: 

<img src="../images/nexus_welcome.png" alt="nexus_welcome" width="400">

## Explore Quay Registry 

A) Confirm you can log into the Quay Container Registry: <https://quay.{{ ocp_app_suffix }}/repository> <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('https://quay.{{ ocp_app_suffix }}/repository')" alt="copy-paste" width="20"></a>

   * Username: `student-{{ student_number }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('student-{{ student_number }}')" alt="copy-paste" width="20"></a>
   * Password: `{{ student_pwd }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('{{ student_pwd }}')" alt="copy-paste" width="20"></a>

     >Note: Click on the **Advanced** button and click on the link: **Proceed to quay.apps.ocpws.kee.vizuri.com (unsafe)**

     >Note: If asked, you may have to confirm your username. So please click on the *Confirm Username* button when prompted
    
    
     <img src="../images/confirm_username.png" alt="confirm_username" width="40%">
    
## Explore SonarQube

A) Confirm you can log into the sonarqube console: <http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}> <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('http://sonarqube-student-{{ student_number }}-cicd.{{ ocp_app_suffix }}')" alt="copy-paste" width="20"></a>

   * Username: `admin` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('admin')" alt="copy-paste" width="20"></a>
   * Password: `admin`

   >Note: If you are prompted to enter a token just click on *Skip this tutorial* in the top right. 
   
   
    <img src="../images/skip_tutorial.png" alt="confirm_username" width="40%">
    
B) Should see the page: 

<img src="../images/sonarqube_welcome.png" alt="sonarqube_welcome" width="500">

## Explore Gogs (git repositories) 

A) Confirm you can log into the gogs git server: <http://gogs.{{ ocp_app_suffix }}> <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('http://gogs.{{ ocp_app_suffix }}')" alt="copy-paste" width="20"></a>

   * Username: `student-{{ student_number }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('student-{{ student_number }}')" alt="copy-paste" width="20"></a>
   * Password: `{{ student_pwd }}` <a href="#0"><img src="../images/copy-paste.jpeg" onclick="copyToClipboard('{{ student_pwd }}')" alt="copy-paste" width="20"></a>

B) You have one repository that we will use for the labs:

   * customer-service - SpringBoot REST Web Service utilized as demo project to orchestrate through our CI/CD process.
