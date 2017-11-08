#!/bin/sh
java=java
if test -n "$JAVA_HOME"; then
    java="$JAVA_HOME/bin/java"
fi
export CLASSPATH=$CLASSPATH
exec "$java" $java_args $JAVA_OPTS -Dbeam.app=$BEAM_APP beam.Beam "$@"
exit 1
