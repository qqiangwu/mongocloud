# Introduction
This is a mesos framework for MongoDB clusters. You can launch both a replica set instanceRequest or a sharded cluster instanceRequest using the framework.

# Status
WIP

# 设计原则
+ 所有Actor组织成树，并且记录在Context中
+ Actor由Parent负责加入及移出Context