package com.vizuri.openshift

class Globals {
	static String imageBase = "quay.kee.vizuri.com"
	static String imageNamespace = "vizuri"
	static String containerRegistry = "https://quay.kee.vizuri.com"
	static String nexusUrl = "http://nexus-cicd.apps.aws-ocp-02.kee.vizuri.com"
	static String ocpAppSuffix = "apps.aws-ocp-02.kee.vizuri.com"
}
//class Globals {
//	static String imageBase = "registry.kee.vizuri.com"
//	static String imageNamespace = "vizuri"
//	static String containerRegistry = "https://registry.kee.vizuri.com"
//	static String nexusUrl = "http://nexus-cicd.apps.ocp-nonprod-01.kee.vizuri.com"
//	static String ocpAppSuffix = "apps.ocp-nonprod-01.kee.vizuri.com"
//}
def init(projectFolder = "./") {
	node ("maven") {
		echo ">>>>>>  Branch Name: " + BRANCH_NAME;
		def release_number;

		if(isRelease()) {
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
			//release_number = "1.0.0-SNAPSHOT"
			//def pom = readMavenPom file: "${projectFolder}/pom.xml"
			//release_number = pom.properties.get("build.number")
			echo "release_number: ${release_number}"
		}

		env.RELEASE_NUMBER = release_number;
	}
}

def isFeature() {
	if(BRANCH_NAME.startsWith("feature")) {
		notifyBuild()
		return true;
	}
	return false;
}
def isRelease() {
	if(BRANCH_NAME.startsWith("release")) {
		notifyBuild()
		return true;
	}
	return false;
}
def isDevelop() {
	if(BRANCH_NAME.startsWith("develop")) {
		notifyBuild()
		return true;
	}
	return false;
}


def buildJava(projectFolder = "./") {
	def nexusUrl = Globals.nexusUrl;
	echo "In buildJava: ${env.RELEASE_NUMBER}"
	stage('Checkout') {
		echo "In checkout"
		checkout scm
	}
	stage('Build') {
		echo "In Build"
		sh "mvn -s configuration/settings.xml -Dnexus.url=${nexusUrl} -f ${projectFolder} -DskipTests=true -Dbuild.number=${env.RELEASE_NUMBER} clean install"
	}
}
def testJava(projectFolder = "./") {
	def nexusUrl = Globals.nexusUrl;
	
	echo "In testJava: ${env.RELEASE_NUMBER}"
	stage ('Unit Test') {
		sh "mvn -s configuration/settings.xml -Dnexus.url=${nexusUrl} -f ${projectFolder} -Dbuild.number=${env.RELEASE_NUMBER} test"
		junit "${projectFolder}/target/surefire-reports/*.xml"

		step([$class: 'XUnitBuilder',
			thresholds: [
				[$class: 'FailedThreshold', unstableThreshold: '1']
			],
			tools: [
				[$class: "JUnitType", pattern: "${projectFolder}/target/surefire-reports/*.xml"]
			]])
	}
}
def integrationTestJava(app_name, ocp_project, projectFolder = "./") {
	echo "In integrationTestJava: ${env.RELEASE_NUMBER}"
	def nexusUrl = Globals.nexusUrl;
	
	def ocpAppSuffix = Globals.ocpAppSuffix;
	def testEndpoint = "http://${app_name}-${ocp_project}.${ocpAppSuffix}"
	stage ('Integration Test') {
		sh "mvn -s configuration/settings.xml -Dnexus.url=${nexusUrl} -f ${projectFolder} -P integration-tests -Dbuild.number=${env.RELEASE_NUMBER} -DbaseUrl=${testEndpoint} integration-test" 
		junit "${projectFolder}/target/surefire-reports/*.xml"

		step([$class: 'XUnitBuilder',
			thresholds: [
				[$class: 'FailedThreshold', unstableThreshold: '1']
			],
			tools: [
				[$class: "JUnitType", pattern: "${projectFolder}/target/surefire-reports/*.xml"]
			]])
	}
}

def analyzeJava(projectFolder = "./") {
	def nexusUrl = Globals.nexusUrl;
	stage('SonarQube Analysis') {		
		withSonarQubeEnv('sonar') { sh "mvn -s configuration/settings.xml -Dnexus.url=${nexusUrl} -f ${projectFolder} -Dbuild.number=${env.RELEASE_NUMBER}  sonar:sonar" }
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

def deployJava(projectFolder = "./") {
	echo "In deployJava: ${env.RELEASE_NUMBER}"
	def nexus_url = Globals.nexusUrl;

	stage('Deploy Build Artifact') {
		echo "In Deploy"
		if(nexus_url != null) {
			sh "mvn -s configuration/settings.xml -f ${projectFolder} -DskipTests=true -Dbuild.number=${env.RELEASE_NUMBER} -Dnexus.url=${nexus_url} deploy"
		}
		else {
			sh "mvn -s configuration/settings.xml -f ${projectFolder} -DskipTests=true -Dbuild.number=${env.RELEASE_NUMBER} deploy"
		}
	}
}
def dockerBuild(app_name, projectFolder = "./") {
	def tag = "${env.RELEASE_NUMBER}"
	stage('Container Build') {
		echo "In DockerBuild: ${app_name}:${tag}"
		docker.withRegistry(Globals.containerRegistry, "docker-credentials") {
			def img = docker.build("${Globals.imageNamespace}/${app_name}:${tag}", "${projectFolder}")
			return img
		}
	}
}
def podmanBuild(app_name, projectFolder = "./") {
	def tag = "${env.RELEASE_NUMBER}"
	stage('Container Build') {
		echo "In DockerBuildOCP: ${app_name}:${tag}"
		sh "podman build -t ${Globals.imageBase}/${Globals.imageNamespace}/${app_name}:${tag} ${projectFolder}"
	}
}
def scanImage(app_name, projectFolder = "./") {
	def tag = "${env.RELEASE_NUMBER}"
	stage('Container Scan') {
		writeFile file: 'anchore_images', text: "${Globals.imageBase}/${Globals.imageNamespace}/${app_name}:${tag} ${projectFolder}/Dockerfile"
		sh 'cat anchore_images'
		anchore name: 'anchore_images'
	}
}
def podmanPush(app_name) {
	def tag = "${env.RELEASE_NUMBER}"
	stage('Container Push') {
		echo "In DockerPushOCP:"
		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'quay-credentials',
			usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
			echo "-u $USERNAME -p $PASSWORD"
			sh "podman login -u ${USERNAME} -p ${PASSWORD} ${Globals.imageBase}"
			sh "podman push ${Globals.imageBase}/${Globals.imageNamespace}/${app_name}:${tag}"
		}
	}
}

def dockerPush(img) {
	stage('Container Push') {
		docker.withRegistry(Globals.containerRegistry, "docker-credentials") {
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
		notify(ocp_project, "Release ${env.RELEASE_NUMBER} of ${app_name} is ready for ${ocp_project}. Promote release here ${JOB_URL}")
		input message: "Do you want to deploy ${app_name} release ${env.RELEASE_NUMBER} to ${ocp_project}?", submitter: "keudy"
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
					//dc = openshift.newApp("-f https://raw.githubusercontent.com/Vizuri/openshift-pipeline-templates/master/templates/springboot-dc.yaml -p IMAGE_NAME=${Globals.imageBase}/${ocp_project}/${app_name}:latest -p APP_NAME=${app_name}").narrow("dc")
					dc = openshift.newApp("-f https://raw.githubusercontent.com/Vizuri/openshift-pipeline-templates/master/templates/springboot-dc.yaml -p IMAGE_NAME=${Globals.imageBase}/${Globals.imageNamespace}/${app_name}:${tag} -p APP_NAME=${app_name}").narrow("dc")
				}
				else {
					def dcObject = dc.object()
					dcObject.spec.template.spec.containers[0].image = "${Globals.imageBase}/${Globals.imageNamespace}/${app_name}:${tag}"
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

