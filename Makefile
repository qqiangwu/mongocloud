build:
	mvn package -Dmaven.test.skip=true

cp: build
	scp target/mm.jar mooc2:~
