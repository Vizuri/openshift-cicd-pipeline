# Exercise 5: Publish Build Artifact
The next step in the process is to publish the build artifact to our Nexus Repository.

* Edit the Jenkinsfile for the *customer-service* project by replacing the *Exercise 5 placeholder*  with the code below. 
  This will add the *Deploy Build Artifact* stages to the build pipeline after the *Quality Gate* stage:

```
	
	stage('Deploy Build Artifact') { 
		sh "mvn -s configuration/settings.xml -DskipTests=true -Dbuild.number=${release_number} -Dnexus.url=${nexusUrl} deploy"	 
	}
	

```

* Rebuild Project by returning to the customer-service develop job and trigger a build.