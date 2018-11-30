package com.vizuri.openshift

def call(body) {
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
      println ">>>> Starting DeliveryPipeline";

      println ">>>>>  Build Number ${BUILD_NUMBER}";

      def image_tag="v1.${BUILD_NUMBER}";
         
      def release_number;


      echo ">>>>>>  Branch Name: " + BRANCH_NAME;
         
         
      if(BRANCH_NAME.startsWith("release")) {
         //def (branch_name, branch_release_number) = BRANCH_NAME.tokenize( '/' )
         def tokens = BRANCH_NAME.tokenize( '/' )
         branch_name = tokens[0]
         branch_release_number = tokens[1]
         
         release_number = branch_release_number
         
         stage('Confirm Deploy?') {
             milestone 1
                 input message: "Do you want to deploy release ${BRANCH_NAME} to test?" 
	         }
      }
      else {
         release_number = pipelineParams.snapshot_release_number
         ocp_project = pipelineParams.ocp_dev_project
      }
      
      node('maven') {	
         stage('Checkout') {
              echo "In checkout" 
    	      checkout scm
         }
         stage('Build') {
              echo "In Build"		
              sh "mvn -s configuration/settings.xml -DskipTests=true -Dbuild.number=${release_number} clean install"
         }

         stage ('test') {
                parallel (
                   "unit tests": { sh 'mvn -s configuration/settings.xml -Dbuild.number=${release_number} test' },
                   "integration tests": { sh 'mvn -s configuration/settings.xml -Dbuild.number=${release_number} integration-test' }
                )
                junit 'target/surefire-reports/*.xml'

                step([$class: 'XUnitBuilder',
                   thresholds: [[$class: 'FailedThreshold', unstableThreshold: '1']],
                   tools: [[$class: 'JUnitType', pattern: 'target/surefire-reports/*.xml']]])
         }

         stage('DockerBuild') {
             echo "In DockerBuild: ${pipelineParams.ocp_cluster} : ${ocp_project}" 
	     openshift.withCluster( "${pipelineParams.ocp_cluster}" ) {
	          openshift.withProject( "${ocp_project}" ) {
		      def bc = openshift.selector("bc", "${pipelineParams.app_name}")
		      echo "BC: " + bc
		      echo "BC Exists: " + bc.exists()
		      if(!bc.exists()) {
		         echo "BC Does Not Exist Creating"
			 bc = openshift.newBuild("--binary=true --strategy=docker --name=${pipelineParams.app_name}")
		      }
		      bc = bc.narrow("bc");
		      bc.startBuild("--from-dir .")
                      bc.logs('-f')
		  }
	      }
	  }	
	  stage('Deploy') {
	     openshift.withCluster( "${pipelineParams.ocp_cluster}" ) {
	         openshift.withProject( "${ocp_project}" ) {
		      echo "In Deploy: ${openshift.project()} : ${ocp_project}"
		      def dc = openshift.selector("dc", "${pipelineParams.app_name}")
		      echo "DC: " + dc
		      echo "DC Exists: " + dc.exists()
		      if(!dc.exists()) {
		            echo "DC Does Not Exist Creating"
		            dc = openshift.newApp("-f https://raw.githubusercontent.com/Vizuri/openshift-pipeline-templates/master/templates/springboot-dc.yaml -p IMAGE_NAME=172.30.1.1:5000/${ocp_project}/${pipelineParams.app_name}:latest -p APP_NAME=${pipelineParams.app_name}")
		      }
		      else {
		            dc = dc.narrow("dc")
		            dc.deploy("--latest")
                            dc.logs('-f')
	              }
		}
             }
	  }
     }
  }
}
