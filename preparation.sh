mvn clean install -Dmaven.test.skip=true -o
mkdir /tmp/nm
rm /tmp/nm/*
for i in $(find . -type d -name 'target'); do # Not recommended, will break on whitespace
    for j in $(find "$i" -type f -name '*.jar'); do # Not recommended, will break on whitespace
        cp "$j" /tmp/nm
    done
done
cd /tmp/nm
rename 's/3.4.3/3.4.4/' *3.4.3.jar
