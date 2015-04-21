# GitFlow Maven Plugin

The Maven plugin for Vincent Driessen's [successful Git branching model](http://nvie.com/posts/a-successful-git-branching-model/).

This plugin use JGit API provided from [Eclipse](https://eclipse.org/jgit/) and Maven commands.

The picture is a little more complex. Imagine two teams working separated on the same source code, there is a project team and support team.
The project team works only with features and bugfixes. The support team works with hotfixes. 

The project team can work in multiple release at the same time. For example, three release on year.

Besides, there are two testing phases, one is held before delivering to the customer and the other is tested by the customer, until the customer approve the release, the source can not go to master branch.

# Installation

The plugin is available from Maven central.

```
#!xml

<build>
   <plugins>
      <plugin>
         <groupId>com.codegik</groupId>
         <artifactId>gitflow-maven-plugin</artifactId>
      </plugin>
   </plugins>
</build>
```


# Goals Overview

```
#!maven

gitflow:init
```
- Is not necessary execute this goal unless you want plugin do it for you.
- Create develop branch.
- Update pom(s) version (1.0.0).
- Create a first tag (1.0.0).
- To execute this goal the current branch must be **master**.
- `Ex: mvn gitflow:init`


```
#!maven

gitflow:start-release
```
- To execute this goal the current branch must be **develop**.
- Start new **release** branch from **develop** and updates pom(s) with release version. 
- `Ex: mvn gitflow:start-release -Dversion=1.4`


```
#!maven

gitflow:finish-release
```
- To execute this goal the current branch must be **develop**.
- Merge **release** branch into **develop**. 
- Increase pom version based on last Tag created. 
- Create a new tag.
- `Ex: mvn gitflow:finish-release -Dversion=1.4`


```
#!maven

gitflow:start-development
```
- Start new development branch from **release**.
- The branch type must be **feature** or **bugfix**.
- `Ex: mvn gitflow:start-development -DfullBranchName=feature/1.4/task3456`
- `Pattern fullBranchName: <branchType=[feature|bugfix]>/<releaseVersion>/<branchName>`


```
#!maven

gitflow:finish-development
```
- Merge branch **development** into **release**.
- `Ex: mvn gitflow:finish-development -DfullBranchName=feature/1.4/task3456`
- `Pattern fullBranchName: <branchType=[feature|bugfix]>/<releaseVersion>/<branchName>`


```
#!maven

gitflow:start-hotfix
```
- Start new **hotfix** branch from **master**.
- Increase the pom version.
- `Ex: mvn gitflow:start-hotfix -DbranchName=issue312`


```
#!maven

gitflow:finish-hotfix
```
- Merge **hotfix** branch into **develop** and **master**.
- Delete hotfix branch.
- `Ex: mvn gitflow:finish-hotfix -DbranchName=issue312`


```
#!maven

gitflow:publish-release
```
- After the **release** was tested by **team** and **customer**, finally the release will be published on branch **master**.
- Find last **tag** from **release** and merge into **master**.
- **Delete all** related branches bugfix, feature and release.
- `Ex: mvn gitflow:publish-release -Dversion=1.4`


```
#!maven

gitflow:build-release
```
- While the first release is coming out, the other release also need to be tested.
- To execute this goal the current branch must be **release** (Ex: release/1.5).
- Merge **develop** into **release** branch.
- Do not create a Tag.
- `Ex: mvn gitflow:build-release -Dversion=1.5`


## Good luck! ##