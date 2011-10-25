set JSHOMEDIR=d:\home\head\gigaspaces-xap-premium-8.0.0-m7
set LOOKUPGROUPS=dank-123

@rem
@rem SINGLE|CLUSTERED. In SINGLE mirror service and DB are not used
@rem  also change the processor pu.xml to use space-nonpersistent.xml
set MODE=CLUSTERED

@rem
@rem TRUE|FALSE
@rem Relevant for CLUSTER. DB and mirror service will be started. Note you will also need to edit processor
@rem pu.xml to import space-nonpersistent.xml or space-persistent.xml accordingly
set PERSISTENCE=FALSE


@rem
@rem relevant for PERSISTENCE when mirror service is used, if true deletes the DB files
set CLEANDB=TRUE


