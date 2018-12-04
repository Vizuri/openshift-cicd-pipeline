#!/usr/bin/groovy
package com.vizuri.openshift


def call(body) {
	def steps = new com.vizuri.openshift.PipelineSteps();
	def pipelineParams= [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = pipelineParams
	body()

	pipeline {
		environment { 
			RELEASE_NUMBER = ""; 			
			OCP_APP_SUFFIX = "";
			IMAGE_BASE = "";
			IMAGE_NAMESPACE = "";
			REGISTRY_USERNAME = "";
			REGISTRY_PASSWORD = "";			
			CONTAINER_REGISTRY = ""
			NEXUS_URL = ""	
		}
		node ("maven-podman") {
			steps.checkout()
			def projectFolder;
			if(pipelineParams.project_folder) {
				echo "setting project_folder: ${pipelineParams.project_folder}"
				projectFolder = pipelineParams.project_folder
			}
			else {
				echo "setting project_folder: default"
				projectFolder = "./"
			}

			try {
				steps.buildJava(projectFolder)
				steps.testJava(projectFolder)
				steps.analyzeJava(projectFolder)

				if (BRANCH_NAME ==~ /(develop|release.*)/) {
					steps.deployJava(projectFolder)
					steps.podmanBuild(pipelineParams.app_name, projectFolder)
					steps.podmanPush(pipelineParams.app_name)
				}

				if (BRANCH_NAME ==~ /(develop)/) {
					steps.deployOpenshift(pipelineParams.ocp_dev_cluster, pipelineParams.ocp_dev_project, pipelineParams.app_name )
					steps.integrationTestJava(pipelineParams.app_name, pipelineParams.ocp_dev_project, projectFolder)
				}
				if (BRANCH_NAME ==~ /(release.*)/) {
					steps.scanImage(pipelineParams.app_name, projectFolder )
					steps.confirmDeploy(pipelineParams.app_name,pipelineParams.ocp_test_project)
					steps.deployOpenshift(pipelineParams.ocp_test_cluster, pipelineParams.ocp_test_project, pipelineParams.app_name  )
					steps.integrationTestJava(pipelineParams.app_name, pipelineParams.ocp_test_project, projectFolder)
					steps.confirmDeploy(pipelineParams.app_name,pipelineParams.ocp_prod_project)
					steps.deployOpenshift(pipelineParams.ocp_prod_cluster, pipelineParams.ocp_prod_project, pipelineParams.app_name  )
					steps.integrationTestJava(pipelineParams.app_name, pipelineParams.ocp_prod_project, projectFolder)
					//steps.mergeCode()
				}
			} catch (e) {
				currentBuild.result = "FAILED"
				throw e
			} finally {
				//steps.notifyBuild(currentBuild.result)
			}
		}
	}
}
