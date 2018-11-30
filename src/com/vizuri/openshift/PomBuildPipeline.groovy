#!/usr/bin/groovy
package com.vizuri.openshift


def call(body) {
	def utils = new com.vizuri.openshift.Utils();
	def pipelineParams= [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = pipelineParams
	body()

	pipeline {
		environment { RELEASE_NUMBER = ""; }
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
					stash name: 'artifacts'
				}
			}

			if(utils.isRelease() ||  utils.isDevelop()) {
				node ('maven') {
					unstash 'artifacts'
					utils.deployJava(projectFolder)
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
