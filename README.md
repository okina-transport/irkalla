# irkalla
Will distribute information about stop change between tiamat, chouette and nabu. 


## Build
`mvn clean install`

## How it works
Irkalla asks Tiamat every 5 minutes if there are modifications on StopPlaces. Tiamat build a netex file with all modifications. 
Irkalla then send this file to chouette, to apply all modifications on chouette Database.  
If there are too many modifications, modification file is paginated with 1000 StopPlaces.


## Change update frequency
Irkalla verifications are made every 5 minutes but it can be changed in ChouetteStopPlaceUpdateRouteBuilder
2 crontab are defined:
- One for delta updates:
```
   @Value("${chouette.sync.stop.place.cron:0 0/5 * * * ?}")
      private String deltaSyncCronSchedule;
```
- One for full updates:
```  
      @Value("${chouette.sync.stop.place.full.cron:0 0 2 * * ?}")
      private String fullSyncCronSchedule;
```


## Run locally (without kubernetes)

```
server.port=10501

server.admin.host=0.0.0.0
server.admin.port=11501

server.context-path=/irkalla/

spring.activemq.broker-url=tcp://activemq:61616
tiamat.url=http://tiamat:2888
chouette.url=http://localhost:8080
etcd.url=http://etcd-client:2379/v2/keys/prod/irkalla

rutebanken.kubernetes.enabled=false
chouette.sync.stop.place.autoStartup=true

spring.jackson.serialization.write-dates-as-timestamps=false