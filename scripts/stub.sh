#!/bin/sh
MYSELF=`which "$0" 2>/dev/null`
export BEAM_APP=$0
[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"
java=java
if test -n "$JAVA_HOME"; then
    java="$JAVA_HOME/bin/java"
fi
exec "$java" $java_args -Dbeam.app=$0 -jar $MYSELF "$@"
exit 1
