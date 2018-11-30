#!/usr/bin/groovy
package com.vizuri.openshift


def call(body) {
	def utils = new com.vizuri.openshift.Utils();
	def pipelineParams= [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = pipelineParams
	body()
	
	pipeline {
		environment {
			RELEASE_NUMBER = "";
		}
		node {
			checkout scm;
		}
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
			println ">>>> Starting JavaDeliveryPipeline";			
			utils.init(projectFolder);	
			echo "utils.isFeature():utils.isRelease():utils.isDevelop():${env.RELEASE_NUMBER}"
		
			if( utils.isFeature() || utils.isDevelop() || utils.isRelease()) {
				node('maven') {
					utils.buildJava(projectFolder)
					utils.testJava(projectFolder)
					utils.analyzeJava(projectFolder)
					stash name: 'artifacts'
				}
			}
			
			if(utils.isRelease() ||  utils.isDevelop()) {
				node ('maven') {
					unstash 'artifacts'
					utils.deployJava(projectFolder)
				}
			}


			if(utils.isDevelop()) {
				node {
					unstash 'artifacts'
					img = utils.dockerBuild(pipelineParams.app_name, projectFolder)					
					utils.dockerPush(img)
					utils.deployOpenshift(pipelineParams.ocp_dev_cluster, pipelineParams.ocp_dev_project, pipelineParams.app_name )
					utils.integrationTestJava(pipelineParams.app_name, pipelineParams.ocp_dev_project, projectFolder)
				}
			}
			if(utils.isRelease()) {
				node {
					unstash 'artifacts'
					img = utils.dockerBuild(pipelineParams.app_name, projectFolder)
					utils.dockerPush(img)
					utils.scanImage(pipelineParams.app_name, projectFolder )	
					utils.confirmDeploy(pipelineParams.app_name,pipelineParams.ocp_test_project)			
					utils.deployOpenshift(pipelineParams.ocp_test_cluster, pipelineParams.ocp_test_project, pipelineParams.app_name  )
					utils.integrationTestJava(pipelineParams.app_name, pipelineParams.ocp_test_project, projectFolder)
					utils.confirmDeploy(pipelineParams.app_name,pipelineParams.ocp_prod_project)			
					utils.deployOpenshift(pipelineParams.ocp_prod_cluster, pipelineParams.ocp_prod_project, pipelineParams.app_name  )
					utils.integrationTestJava(pipelineParams.app_name, pipelineParams.ocp_prod_project, projectFolder)	
					//utils.mergeCode()
				}
			}
		} catch (e) {
			currentBuild.result = "FAILED"
			throw e
		} finally {
			utils.notifyBuild(currentBuild.result)
		}
	}
}
