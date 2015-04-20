# GitFlow Maven Plugin

The Maven plugin for Vincent Driessen's [successful Git branching model](http://nvie.com/posts/a-successful-git-branching-model/).

This plugin use JGit API provided from [Eclipse](https://eclipse.org/jgit/) and Maven commands.

The picture is a little more complex. Imagine two teams working separated on the same source code, there is a project team and support team.
The project team works only with features and bug fixes. The support team works with hotfixes. 

The project team can work in multiple release at the same time. For example, three release on year.

Besides, there are two testing phases, one is held before delivering to the customer and the other is tested by the customer, until the customer approve the release, the source can not go to master branch.

# Installation

The plugin is available from Maven central.

# Goals Overview

- `gitflow:start-release` - Start new **release** branch from **develop** and updates pom(s) with release version. To execute this goal the current branch must be **develop**.

- `gitflow:finish-release` - Merge **release** branch into **develop**. Increase pom version based on last Tag created. To execute this goal the current branch must be **develop**.
- `gitflow:start-development` - Starts a feature branch and updates pom(s) with feature name.
- `gitflow:finish-development` - Merges a feature branch.
- `gitflow:start-hotfix` - Starts a hotfix branch and updates pom(s) with hotfix version.
- `gitflow:finish-hotfix` - Merges a hotfix branch.


**Good luck!**