from fabric.api import local, env, cd
from fabric.decorators import *
from fabric.operations import *

env.hosts = ['reins@192.168.1.20']
env.password = 123456
env.warn_only = True

work_dir = 'mongocloud'

@hosts("localhost")
def pack():
    local('mvn package -Dmaven.test.skip')

@hosts("localhost")
def remoteUpload():
    local('scp target/mongocloud.jar mooc2:~/mongocloud')

def upload():
    run('mkdir -p {}'.format(work_dir))

    with cd(work_dir):
        put('bin/*', '.')
        put('target/mongocloud.jar', '.')

def launch():
    with cd(work_dir):
        run('LIBPROCESS_IP=192.168.1.20 java -jar mongocloud.jar | tee cloud.log')