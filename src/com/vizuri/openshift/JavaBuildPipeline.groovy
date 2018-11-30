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
			OCP_APP_SUFFIX = pipelineParams.ocpAppSuffix;
			IMAGE_BASE = pipelineParams.imageBase;
			IMAGE_NAMESPACE = pipelineParams.imageBase;
			REGISTRY_USERNAME = pipelineParams.registryUsername;
			REGISTRY_PASSWORD = pipelineParams.registryPassword;			
			CONTAINER_REGISTRY = "https://${pipelineParams.imageBase}"
			NEXUS_URL = "http://nexus-${REGISTRY_USERNAME}-cicd.${ocpAppSuffix}"	
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
