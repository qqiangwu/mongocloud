from fabric.api import local, lcd

def build():
    local('mvn package -Dmaven.test.skip=true')

def send():
    build()
    local('scp target/mm.jar reins:~')

def run():
    local('ssh reins "LIBPROCESS_IP=192.168.1.64 java -jar mm.jar"')

def all():
    send()
    run()
