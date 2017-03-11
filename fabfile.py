from fabric.api import local, lcd

def build():
    local('mvn package -Dmaven.test.skip=true')

def send():
    build()
    local('scp target/mm.jar mooc2:~')

def run():
    local('ssh mooc2 "LIBPROCESS_IP=192.168.1.20 java -jar mm.jar"')

def all():
    send()
    run()
