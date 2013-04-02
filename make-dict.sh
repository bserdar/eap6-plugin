#/bin/sh

# Run this under modules/ directory. It generates an initial ddictionary file that needs some editing

root="/usr/share/jbossas/modules"
for f in `find $root -name '*.jar'` ; do
    dir=${f#$root}
    fname=`basename $f`
    modulename=`echo ${dir%$fname}|sed 's/\/main\///'|cut -b 2-|sed 's/\//./g'`
    rm -rf META-INF
    jar xf $f META-INF
    pom=`find META-INF -name pom.xml`
    pom=`echo $pom|sed 's/META-INF\/maven\///'|sed 's/\/pom.xml//'|sed 's/\//:/g'`
    rm -rf META-INF
    echo $pom=$modulename
done
