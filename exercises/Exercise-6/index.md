# Exercise 6 - Container Build
Now we are going to build our container and publish it to our enterprise registry (Quay).

We will be using podman to build our container.

Add the following variables to the top of the Jenkinsfile.

```

def imageBase = "quay.{{ ocp_app_suffix }}";
def imageNamespace = "student_{{ student_number }}";
def registryUsername = "student-{{ student_number }}"
def registryPassword = "{{ student_pwd }}"
```

Add the following steps to the Jenkinsfile.

```
	def tag = "${release_number}"
	
	if (BRANCH_NAME ==~ /(develop|release.*)/) {		
		stage('Container Build') { 
			sh "podman build -t ${imageBase}/${imageNamespace}/${app_name}:${tag} ." 
		}
		
		stage("Container Push") {
			if (BRANCH_NAME ==~ /(develop|release.*)/) {
				sh "podman login -u ${registryUsername} -p ${registryPassword} ${imageBase}"
				sh "podman push ${imageBase}/${imageNamespace}/${app_name}:${tag}"
			}
		}
	}
```

If you log into the Quay registry, you will see your image.

https://quay.{{ ocp_app_suffix }}
* Username: student-{{ student_number }}
* Password: {{ student_pwd }}

Click on the customer repository then browse the tags.  Notice the Security Scan tag.  The image is queued for scanning.  Once complete you will see the results of the scan.  
