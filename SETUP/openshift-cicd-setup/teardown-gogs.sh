---
- name: Setup CI/CD Workshop Class
  hosts: config
  become: true

  tasks:
  - name: Delete CICD Projects
    shell: |
      oc delete project gogs
