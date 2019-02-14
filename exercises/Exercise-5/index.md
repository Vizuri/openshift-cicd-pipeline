# Exercise 5: Publish Build Artifact
The next step in the process is to publish the build artifact to our Nexus Repository.

Add the following stage to the Jenkins file.

```
...
	stage('Deploy Build Artifact') { 
		sh "mvn -s configuration/settings.xml -DskipTests=true -Dbuild.number=${release_number} -Dnexus.url=${nexusUrl} deploy"	 
	}
...

```