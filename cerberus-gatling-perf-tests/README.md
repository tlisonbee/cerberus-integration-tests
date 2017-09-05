# Cerberus Gatling Performance Tests

[Gatling](http://gatling.io/) based performance tests

## Parameters

Each simulation requires different parameters.

## Parameters for IamPrincipalAuthAndReadSimulation

These properties can be supplied to the simulation via environment variables or system properties

Parameter | Description
--------- | ---------------
CERBERUS_API_URL | This controls the api that will be perfromance tested
CERBERUS_ACCOUNT_ID | The account id is needed when creating iam roles for the test and granting kms decrypt for the cerberus account
REGION | The region to auth with Cerberus and use KMS in
NUMBER_OF_SERVICES_FOR_SIMULATION | This is the number of SDBs with random data will be created for the simulation. Each simulated user will be fed one of these services randomly to be.
CREATE_IAM_ROLES | This defaults to false. Setting this to true creates a new iam role that will get deleted at the end of the simulation for each NUMBER_OF_SERVICES_FOR_SIMULATION WARNING: This creates a KMS key for each IAM role that does not get cleaned up automatically. Setting this to false makes each simulated service use the role that is running the tests.
PEAK_USERS | The peak number of simulated concurrent users for the test
RAMP_UP_TIME_IN_MINUTES | The amount of time to ramp up from peak users to 0 users.
HOLD_TIME_AFTER_PEAK_IN_MINUTES | The amount of minutes to hold the peak users for

E.g. in a Bash terminal

```bash
   # Set required params
   export CERBERUS_API_URL="https://dev.cerberus-oss.io/"
   export CERBERUS_ACCOUNT_ID=1234567890
   export REGION="us-east-1"
   
   # Set optional parameters
   export NUMBER_OF_SERVICES_FOR_SIMULATION=1
   export CREATE_IAM_ROLES=false
   export PEAK_USERS=1
   export RAMP_UP_TIME_IN_MINUTES=1
   export HOLD_TIME_AFTER_PEAK_IN_MINUTES=1
```

## Running Locally

First export required parameters and AWS credentials.

Use the following gradlew task to run the default simulation:

    ./gradlew clean cerberus-gatling-perf-tests:runSimulation

To specify the simulation name:

    ./gradlew cerberus-gatling-perf-tests:runSimulation -Psimulation=VaultDirectSimulation
    
To determine your current account id and role use:

    aws sts get-caller-identity
    
## Running the Fat Jar

The following gradle task can create a fat jar containing the tests

    ./gradlew clean cerberus-gatling-perf-tests:gatlingCompileSimulationFatJar
    
You can trigger the tests via 

    java -jar PATH/TO/JAR io.gatling.app.Gatling --simulation SIMULATION NAME -rf PATH/TO/SAVE/REPORT
    

Additional Gatling [configuration parameters](https://github.com/gatling/gatling/blob/master/gatling-core/src/main/resources/gatling-defaults.conf) are available, e.g.

    -Dgatling.http.ahc.connectTimeout=60000 -Dgatling.http.ahc.handshakeTimeout=60000 -Dgatling.http.ahc.requestTimeout=120000 -Dgatling.http.ahc.keepAlive=false    
    
See the [Gatling docs](http://gatling.io/docs/current/) for more information
