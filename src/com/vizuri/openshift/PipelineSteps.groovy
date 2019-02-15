package com.vizuri.openshift


def setEnv(pipelineParams) {
	env.OCP_APP_SUFFIX = pipelineParams.ocpAppSuffix;
	env.IMAGE_BASE = pipelineParams.imageBase;
	env.IMAGE_NAMESPACE = pipelineParams.imageNamespace;
	env.REGISTRY_USERNAME = pipelineParams.registryUsername;
	env.REGISTRY_PASSWORD = pipelineParams.registryPassword;
	env.CONTAINER_REGISTRY = "https://${pipelineParams.imageBase}"
	env.NEXUS_URL = "http://nexus-${pipelineParams.registryUsername}-cicd.${pipelineParams.ocpAppSuffix}"
}

def checkout() {
	echo "In checkout"
	stage('Checkout') {

		checkout scm

		if(BRANCH_NAME ==~ /(release.*)/) {
			def tokens = BRANCH_NAME.tokenize( '/' )
			branch_name = tokens[0]
			branch_release_number = tokens[1]
			release_number = branch_release_number
		}
		else {
			sh (
					script: "mvn -B help:evaluate -Dexpression=project.version | grep -e '^[^\\[]' > release.txt",
					returnStdout: true,
					returnStatus: false
					)
			release_number = readFile('release.txt').trim()
			echo "release_number: ${release_number}"
		}
		env.RELEASE_NUMBER = release_number;
	}
}



def buildJava(projectFolder = ".") {
	def nexusUrl = env.NEXUS_URL;
	echo "In buildJava: ${env.RELEASE_NUMBER} : ${env.NEXUS_URL}"

	stage('Build') {
		echo "In Build"
		sh "mvn -s configuration/settings.xml -Dnexus.url=${nexusUrl} -f ${projectFolder}/pom.xml -DskipTests=true -Dbuild.number=${env.RELEASE_NUMBER} clean install"
	}
}
def testJava(projectFolder = ".") {
	def nexusUrl = env.NEXUS_URL;

	echo "In testJava: ${env.RELEASE_NUMBER}"
	stage ('Unit Test') {
		sh "mvn -s configuration/settings.xml -Dnexus.url=${nexusUrl} -f ${projectFolder}/pom.xml -Dbuild.number=${env.RELEASE_NUMBER} test"
		
		sh "ls ${projectFolder}/target/surefire-reports/*.xml"
		
		junit "target/surefire-reports/*.xml"
		
		echo "After Junit"

		step([$class: 'XUnitBuilder',
			thresholds: [
				[$class: 'FailedThreshold', unstableThreshold: '1']
			],
			tools: [
				[$class: "JUnitType", pattern: "target/surefire-reports/*.xml"]
			]])
	}
}
def integrationTestJava(app_name, ocp_project, projectFolder = ".") {
	echo "In integrationTestJava: ${env.RELEASE_NUMBER}"
	def nexusUrl = env.NEXUS_URL;

	def ocpAppSuffix = env.OCP_APP_SUFFIX;
	def testEndpoint = "http://${app_name}-${ocp_project}.${ocpAppSuffix}"
	stage ('Integration Test') {
		sh "mvn -s configuration/settings.xml -Dnexus.url=${nexusUrl} -f ${projectFolder}/pom.xml -P integration-tests -Dbuild.number=${env.RELEASE_NUMBER} -DbaseUrl=${testEndpoint} integration-test"
		junit "target/surefire-reports/*.xml"

		step([$class: 'XUnitBuilder',
			thresholds: [
				[$class: 'FailedThreshold', unstableThreshold: '1']
			],
			tools: [
				[$class: "JUnitType", pattern: "target/surefire-reports/*.xml"]
			]])
	}
}

def analyzeJava(projectFolder = ".") {
	def nexusUrl = env.NEXUS_URL;
	stage('SonarQube Analysis') {
		withSonarQubeEnv('sonar') { sh "mvn -s configuration/settings.xml -Dnexus.url=${nexusUrl} -f ${projectFolder}/pom.xml -Dbuild.number=${env.RELEASE_NUMBER}  sonar:sonar" }
	}


	stage("Quality Gate"){
		timeout(time: 1, unit: 'HOURS') {
			def qg = waitForQualityGate()
			if (qg.status != 'OK') {
				error "Pipeline aborted due to quality gate failure: ${qg.status}"
			}
		}
	}
}

def deployJava(projectFolder = ".") {
	echo "In deployJava: ${env.RELEASE_NUMBER}"
	def nexus_url = env.NEXUS_URL;

	stage('Deploy Build Artifact') {
		echo "In Deploy"
		if(nexus_url != null) {
			sh "mvn -s configuration/settings.xml -f ${projectFolder}/pom.xml -DskipTests=true -Dbuild.number=${env.RELEASE_NUMBER} -Dnexus.url=${nexus_url} deploy"
		}
		else {
			sh "mvn -s configuration/settings.xml -f ${projectFolder}/pom.xml -DskipTests=true -Dbuild.number=${env.RELEASE_NUMBER} deploy"
		}
	}
}
def dockerBuild(app_name, projectFolder = ".") {
	def tag = "${env.RELEASE_NUMBER}"
	stage('Container Build') {
		echo "In DockerBuild: ${app_name}:${tag}"
		docker.withRegistry(env.CONTAINER_REGISTRY, "docker-credentials") {
			def img = docker.build("${env.IMAGE_NAMESPACE}/${app_name}:${tag}", "${projectFolder}")
			return img
		}
	}
}
def podmanBuild(app_name, projectFolder = ".") {
	def tag = "${env.RELEASE_NUMBER}"
	stage('Container Build') {
		echo "In DockerBuildOCP: ${app_name}:${tag}"
		sh "podman build -t ${env.IMAGE_BASE}/${env.IMAGE_NAMESPACE}/${app_name}:${tag} ${projectFolder}"
	}
}
def scanImage(app_name, projectFolder = ".") {
	def tag = "${env.RELEASE_NUMBER}"
	stage('Container Scan') {
		writeFile file: 'anchore_images', text: "${env.IMAGE_BASE}/${env.IMAGE_NAMESPACE}/${app_name}:${tag} ${projectFolder}/Dockerfile"
		anchore engineRetries: '1000', name: 'anchore_images'
	}
}
def podmanPush(app_name) {
	def tag = "${env.RELEASE_NUMBER}"
	stage('Container Push') {
		echo "In DockerPushOCP:"
		sh "podman login -u ${REGISTRY_USERNAME} -p ${REGISTRY_PASSWORD} ${env.IMAGE_BASE}"
		sh "podman push ${env.IMAGE_BASE}/${env.IMAGE_NAMESPACE}/${app_name}:${tag}"
	}
}
def podmanPushCred(app_name) {
	def tag = "${env.RELEASE_NUMBER}"
	stage('Container Push') {
		echo "In DockerPushOCP:"
		withCredentials([
			[$class: 'UsernamePasswordMultiBinding', credentialsId: 'quay-credentials',
				usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']
		]) {
			echo "-u $USERNAME -p $PASSWORD"
			sh "podman login -u ${USERNAME} -p ${PASSWORD} ${env.IMAGE_BASE}"
			sh "podman push ${env.IMAGE_BASE}/${env.IMAGE_NAMESPACE}/${app_name}:${tag}"
		}
	}
}
def dockerPush(img) {
	stage('Container Push') {
		docker.withRegistry(env.CONTAINER_REGISTRY, "docker-credentials") {
			echo "In DockerPush:"
			img.push()
		}
	}
}

def dockerBuildOpenshift(ocp_cluster, ocp_project, app_name) {
	stage('Container Build') {
		echo "In DockerBuild: ${ocp_cluster} : ${ocp_project}"
		openshift.withCluster( "${ocp_cluster}" ) {
			openshift.withProject( "${ocp_project}" ) {
				def bc = openshift.selector("bc", "${app_name}")
				echo "BC: " + bc
				echo "BC Exists: " + bc.exists()
				if(!bc.exists()) {
					echo "BC Does Not Exist Creating"
					bc = openshift.newBuild("--binary=true --strategy=docker --name=${app_name}").narrow("bc")
				}
				//bc = bc.narrow("bc");
				def builds = bc.startBuild("--from-dir .")

				builds.logs('-f')

				echo("BUILD Finished")

				timeout(5) {
					builds.untilEach(1) {
						echo "In Look for bc status:" + it.count() + ":" + it.object().status.phase
						if(it.object().status.phase == "Failed") {
							currentBuild.result = 'FAILURE'
							error("Docker Openshift Build Failed")
						}
						return (it.object().status.phase == "Complete")
					}
				}



			}
		}
	}
}
def confirmDeploy(app_name, ocp_project) {
	stage("Confirm Deploy to ${ocp_project}?") {
		//notify(ocp_project, "Release ${env.RELEASE_NUMBER} of ${app_name} is ready for ${ocp_project}. Promote release here ${JOB_URL}")
		//input message: "Do you want to deploy ${app_name} release ${env.RELEASE_NUMBER} to ${ocp_project}?", submitter: "keudy"
		input message: "Do you want to deploy ${app_name} release ${env.RELEASE_NUMBER} to ${ocp_project}?"
	}
}


def deployOpenshift(ocp_cluster, ocp_project, app_name) {
	def tag = "${env.RELEASE_NUMBER}"

	stage("Deploy Openshift ${ocp_project}") {
		echo "In Deploy: ${ocp_cluster} : ${ocp_project} : ${app_name}"
		openshift.withCluster( "${ocp_cluster}" ) {
			openshift.withProject( "${ocp_project}" ) {
				def dc = openshift.selector("dc", "${app_name}")
				echo "DC: " + dc
				echo "DC Exists: " + dc.exists()
				if(!dc.exists()) {
					echo "DC Does Not Exist Creating"
					//dc = openshift.newApp("-f https://raw.githubusercontent.com/Vizuri/openshift-pipeline-templates/master/templates/springboot-dc.yaml -p IMAGE_NAME=${env.IMAGE_BASE}/${ocp_project}/${app_name}:latest -p APP_NAME=${app_name}").narrow("dc")
					dc = openshift.newApp("-f https://raw.githubusercontent.com/Vizuri/openshift-cicd-pipeline/master/templates/springboot-dc.yaml -p IMAGE_NAME=${env.IMAGE_BASE}/${env.IMAGE_NAMESPACE}/${app_name}:${tag} -p APP_NAME=${app_name}").narrow("dc")
				}
				else {
					def dcObject = dc.object()
					dcObject.spec.template.spec.containers[0].image = "${env.IMAGE_BASE}/${env.IMAGE_NAMESPACE}/${app_name}:${tag}"
					openshift.apply(dcObject)
				}

				def rm = dc.rollout()
				rm.latest()
				timeout(5) {
					def latestDeploymentVersion = openshift.selector('dc',"${app_name}").object().status.latestVersion
					echo "Got LatestDeploymentVersion:" + latestDeploymentVersion
					def rc = openshift.selector('rc', "${app_name}-${latestDeploymentVersion}")
					echo "Got RC" + rc
					rc.untilEach(1){
						def rcMap = it.object()
						return (rcMap.status.replicas.equals(rcMap.status.readyReplicas))
					}
				}
			}
		}
	}
}
def mergeCode() {
	def releaseBranch = "release/${env.RELEASE_NUMBER}"
	echo "Merging ${releaseBranch}"
	stage('Merge Release') {
		sh "git branch"
		sh "git checkout master"
		sh "git pull origin master"
		sh "git branch -a"
		//sh "git merge ${releaseBranch}"
		//sh "git push origin master"
	}
}

def getSlackToken(channel) {
	def token;
	if(channel.equals("cicd-feature")) {
		token = "PsY21OKCkPM5ED01xurKwQkq";
	}
	else if (channel.equals("cicd-develop")) {
		token = "PsY21OKCkPM5ED01xurKwQkq";
	}
	else if (channel.equals("cicd-test")) {
		token = "dMQ7l26s3pb4qa4AijxanODC";
	}
	else if (channel.equals("cicd-prod")) {
		token = "HW5G7kmVdRU6XyDJrcKvdyQA";
	}
	return token;
}

def notify(ocp_project, message) {
	def channel;
	if(ocp_project.contains("test")) {
		channel = "cicd-test"
	}
	else if(ocp_project.contains("prod")) {
		channel = "cicd-prod"
	}

	def token = getSlackToken(channel);
	slackSend color: "good", channel: channel, token: token, message: message
}

def notifyBuild(String buildStatus = 'STARTED') {
	// build status of null means successful
	buildStatus =  buildStatus ?: 'SUCCESSFUL'

	echo "In notifyBuild ${buildStatus} : ${BRANCH_NAME}"

	def buildType;
	def channel;
	def token = getSlackToken(channel);

	if(BRANCH_NAME.startsWith("feature")) {
		buildType = "Feature"
		channel = "cicd-develop"
	}
	else if(BRANCH_NAME.startsWith("develop")) {
		buildType = "Develop"
		channel = "cicd-develop"
	}
	else if(BRANCH_NAME.startsWith("release")) {
		buildType = "Release"
		channel = "cicd-test"
	}

	if (buildStatus == 'STARTED') {
		slackSend color: "good", channel: channel, token: token, message: "${buildType} Job: ${BRANCH_NAME} with buildnumber ${env.BUILD_NUMBER} was started"
	} else if (buildStatus == 'SUCCESS') {
		slackSend color: "good", channel: channel, token: token, message: "${buildType} Job: ${BRANCH_NAME} with buildnumber ${env.BUILD_NUMBER} completed successfully"
	} else {
		slackSend color: "danger", channel: channel, token: token, message: "${buildType} Job: ${BRANCH_NAME} with buildnumber ${env.BUILD_NUMBER} failed"
	}

}

