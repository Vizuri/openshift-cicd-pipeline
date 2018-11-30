#!/usr/bin/groovy
package com.vizuri.openshift


def call(body) {
	def utils = new com.vizuri.openshift.Utils();
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
			def tokens = BRANCH_NAME.tokenize( '/' )
			branch_name = tokens[0]
			branch_release_number = tokens[1]

			release_number = branch_release_number

			stage('Confirm Deploy?') {
				milestone 1
				input message: "Do you want to deploy release ${BRANCH_NAME} to test?", submitter: "developer"
			}
		}
		else {
			release_number = pipelineParams.snapshot_release_number
			ocp_project = pipelineParams.ocp_dev_project
		}

		node('maven') {
			utils.buildJava(release_number)
			utils.testJava(release_number)
			utils.deployJava(release_number,null)
			utils.dockerBuildOpenshift(pipelineParams.ocp_cluster, ocp_project, pipelineParams.app_name )
			utils.deployOpenshift(pipelineParams.ocp_cluster, ocp_project, pipelineParams.app_name )
		}
	}
}
