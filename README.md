# customer-ms-spring: Spring Boot Microservice with CouchDB Database
[![Build Status](https://travis-ci.org/ibm-cloud-architecture/refarch-cloudnative-micro-customer.svg?branch=master)](https://travis-ci.org/ibm-cloud-architecture/refarch-cloudnative-micro-customer)

*This project is part of the `IBM Cloud Native Reference Architecture` suite, available at
https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/tree/spring*

## Table of Contents
  * [Introduction](#introduction)
    + [APIs](#apis)
  * [Pre-requisites:](#pre-requisites)
  * [Validate the Customer Microservice API](#validate-the-customer-microservice-api)
    + [Setup](#setup)
      - [1. Create a temporary HS256 shared secret](#1-create-a-temporary-hs256-shared-secret)
      - [2. Generate a JWT Token with `admin` Scope](#2-generate-a-jwt-token-with-admin-scope)
      - [3. Generate a JWT Token with `admin` Scope](#3-generate-a-jwt-token-with-admin-scope)
      c. 
    + [Create a Customer](#create-a-customer)
    + [Search the Customer](#search-the-customer)
    + [Get the Customer](#get-the-customer)
      - [Generate a JWT Token with `blue` Scope for New User](#generate-a-jwt-token-with-blue-scope-for-new-user)
      - [Use `blue` Scoped JWT Token to Get the Customer information](#use-blue-scoped-jwt-token-to-get-the-customer-information)
    + [Delete the User](#delete-the-user)
  * [Deploy Customer Application on Docker](#deploy-customer-application-on-docker)
    + [Deploy the CouchDB Docker Container](#deploy-the-couchdb-docker-container)
    + [Deploy the Customer Docker Container](#deploy-the-customer-docker-container)
  * [Run Customer Service application on localhost](#run-customer-service-application-on-localhost)
  * [Optional: Setup CI/CD Pipeline](#optional-setup-cicd-pipeline)
  * [Conclusion](#conclusion)
  * [Contributing](#contributing)
    + [Contributing a New Chart Package to Microservices Reference Architecture Helm Repository](#contributing-a-new-chart-package-to-microservices-reference-architecture-helm-repository)

## Introduction
This project will demonstrate how to deploy a Spring Boot Application with a CouchDB database onto a Kubernetes Cluster.

![Application Architecture](static/customer.png?raw=true)

Here is an overview of the project's features:
- Leverage [`Spring Boot`](https://projects.spring.io/spring-boot/) framework to build a Microservices application.
- Uses [`Spring Data JPA`](http://projects.spring.io/spring-data-jpa/) to persist data to CouchDB database.
- Uses [`CouchDB`](http://couchdb.apache.org/) as the customer database.
- Uses [`Docker`](https://docs.docker.com/) to package application binary and its dependencies.

        
## APIs
The Customer Microservice REST API is OAuth protected.

![Swagger](static/swagger.png?raw=true)

You can view the API by running `appsody run` and visiting http://localhost:8080/swagger-ui.html#/

## Pre-requisites:
* Create a Kubernetes Cluster by following the steps [here](https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes#create-a-kubernetes-cluster).
* Install the following CLI's on your laptop/workstation:
    + [`docker`](https://docs.docker.com/install/)
    + [`kubectl`](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
    + [`appsody`](https://appsody.dev/docs/installing/installing-appsody/)
    
* Clone customer repository:
```bash
git clone https://github.com/ibm-garage-ref-storefront/customer-ms-spring
cd customer-ms-spring
```

## Validate the Customer Microservice API
Now that we have the customer service up and running, let's go ahead and test that the API works properly.

### Setup
#### a. Setup Customer Service Hostname and Port
To make going through this document easier, we recommend you create environment variables for the customer service hostname/IP and port. To do so, run the following commands:
```bash
export CUSTOMER_HOST=localhost
export CUSTOMER_PORT=8080
export COUCHDB_PROTOCOL=http
export COUCHDB_USER=admin
export COUCHDB_PASSWORD=passw0rd
export COUCHDB_HOST=127.0.0.1
export COUCHDB_PORT=5985
export COUCHDB_DATABASE=customers
```

Where:
* `CUSTOMER_HOST` is the hostname or IP address for the customer service.
  + If using `IBM Cloud Private`, use the IP address of one of the proxy nodes.
  + If using `IBM Cloud Kubernetes Service`, use the IP address of one of the worker nodes.
* `CUSTOMER_PORT` is the port for the customer service.
  + If using `IBM Cloud Private` or `IBM Cloud Kubernetes Service`, enter the value of the NodePort.

#### b. Create a temporary HS256 shared secret
As the APIs in this microservice as OAuth protected, the HS256 shared secret used to sign the JWT generated by the [Authorization Server](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth) is needed to validate the access token provided by the caller.

To make things easier for you, we pasted below the 2048-bit secret that's included in the customer chart [here](chart/customer/values.yaml#L28), which you can export to your environment as follows:
```bash
export HS256_KEY="E6526VJkKYhyTFRFMC0pTECpHcZ7TGcq8pKsVVgz9KtESVpheEO284qKzfzg8HpWNBPeHOxNGlyudUHi6i8tFQJXC8PiI48RUpMh23vPDLGD35pCM0417gf58z5xlmRNii56fwRCmIhhV7hDsm3KO2jRv4EBVz7HrYbzFeqI45CaStkMYNipzSm2duuer7zRdMjEKIdqsby0JfpQpykHmC5L6hxkX0BT7XWqztTr6xHCwqst26O0g8r7bXSYjp4a"
```

However, if you must create your own 2048-bit secret, one can be generated using the following command:
```bash
cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 256 | head -n 1 | xargs echo -n
```

Note that if the [Authorization Server](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth) is also deployed, it must use the *same* HS256 shared secret.

#### c. Compile and test the java application 
You can run the application with appsody such as
`appsody run`
`appsody test`

#### d. Generate a JWT Token with `admin` Scope

Where:
* `admin` is the scope needed to create the user.
* `${TEST_USER}` is the user to create, i.e. `foo`.
* `${HS256_KEY}` is the 2048-bit secret from the previous step.

To generate a JWT Token with an `admin` scope, which will let you create/get/delete users, run the commands below:
```bash
# JWT Header
jwt1=$(echo -n '{"alg":"HS256","typ":"JWT"}' | openssl enc -base64);
# JWT Payload
jwt2=$(echo -n "{\"scope\":[\"admin\"],\"user_name\":\"${TEST_USER}\"}" | openssl enc -base64);
# JWT Signature: Header and Payload
jwt3=$(echo -n "${jwt1}.${jwt2}" | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
# JWT Signature: Create signed hash with secret key
jwt4=$(echo -n "${jwt3}" | openssl dgst -binary -sha256 -hmac "${HS256_KEY}" | openssl enc -base64 | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
# Complete JWT
jwt=$(echo -n "${jwt3}.${jwt4}");
```

### 1. Create a Customer
Let's create a new customer with username `foo` and password `bar` and its respective profile with the following command:

Where:
* `${CUSTOMER_HOST}` is the hostname/ip address for the customer microservice.
* `${CUSTOMER_PORT}` is the port for the customer microservice.
* `${jwt}` is the JWT token created in the previous step.
* `${TEST_USER}` is the user to create, i.e. `foo`.

```bash
curl -X POST -i "http://${CUSTOMER_HOST}:${CUSTOMER_PORT}/customer/add" -H "Content-Type: application/json" -H "Authorization: Bearer ${jwt}" -d "{\"username\": \"${TEST_USER}\", \"password\": \"bar\", \"firstName\": \"foo\", \"lastName\": \"bar\", \"email\": \"foo@bar.com\"}"

HTTP/1.1 201 Created
Date: Mon, 20 Aug 2018 21:43:51 GMT
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
X-Application-Context: customer-microservice:8082
Location: http://localhost:8080/customer/41757d0170344f9ea47a2d9634bc9ba7
Content-Length: 0
Server: Jetty(9.2.13.v20150730)
```

Note the `Location` header returned, which contains the `CUSTOMER_ID` of the created customer.  For the GET calls below, copy the ID in the `Location` header (e.g. in the above, `41757d0170344f9ea47a2d9634bc9ba7`). This id will be used later when deleting the user. To save it in your environment, run the following command using the the id returned above:
```bash
# In this case, we are using the id that was returned in our sample command above, which will differ for you
CUSTOMER_ID=41757d0170344f9ea47a2d9634bc9ba7
```

### 2. Search the Customer
To search users with a particular username, i.e. `foo`, run the command below:__
Where:
* `${CUSTOMER_HOST}` is the hostname/ip address for the customer microservice.
* `${CUSTOMER_PORT}` is the port for the customer microservice.
* `${jwt}` is the JWT token created in the previous step.
* `${TEST_USER}` is the user to create, i.e. `foo`.

```bash
curl -s -X GET "http://${CUSTOMER_HOST}:${CUSTOMER_PORT}/customer/search?username=${TEST_USER}" -H 'Content-type: application/json' -H "${jwt}"

[{"username":"foo","password":"bar","firstName":"foo","lastName":"bar","email":"foo@bar.com","imageUrl":null,"customerId":"7145e43859764b3e8abc76784f1eb36a"}]
```

### 3. Get the Customer
To use the customer service as a non-admin user and still be able to retrieve a user's own record, you must create a JWT token with the `blue` scope and pass the customer id as the value for the `user_name` payload. By doing this, we guarantee that only the identied user can retrieve/update/delete it's own record.

#### Generate a JWT Token with `blue` Scope for New Customer
In order for the newly created user to retrieve its own record, and only its own record, you will need to create a new JWT token with the scope `blue` and a payload that has the `CUSTOMER_ID` as the value for `user_name`. To generate the new JWT token, run the following commands:
Where:
* `blue` is the scope needed to create the user.
* `${CUSTOMER_ID}` is the id of the customer user crated earlier, i.e. `41757d0170344f9ea47a2d9634bc9ba7`.
* `${HS256_KEY}` is the 2048-bit secret from the previous step.

```bash
# JWT Header
jwt1=$(echo -n '{"alg":"HS256","typ":"JWT"}' | openssl enc -base64);
# JWT Payload
jwt2=$(echo -n "{\"scope\":[\"blue\"],\"user_name\":\"${CUSTOMER_ID}\"}" | openssl enc -base64);
# JWT Signature: Header and Payload
jwt3=$(echo -n "${jwt1}.${jwt2}" | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
# JWT Signature: Create signed hash with secret key
jwt4=$(echo -n "${jwt3}" | openssl dgst -binary -sha256 -hmac "${HS256_KEY}" | openssl enc -base64 | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
# Complete JWT
jwt_blue=$(echo -n "${jwt3}.${jwt4}");
```

#### Use `blue` Scoped JWT Token to Retrieve the Customer Record
To retrieve the customer record using the `blue` scoped JWT token, run the command below:
```bash
curl -s -X GET "http://${CUSTOMER_HOST}:${CUSTOMER_PORT}/customer" -H "Authorization: Bearer ${jwt_blue}"

[{"username":"foo","password":"bar","firstName":"foo","lastName":"bar","email":"foo@bar.com","imageUrl":null,"customerId":"7145e43859764b3e8abc76784f1eb36a"}]
```

Note that *only* the customer object identified by the encoded `user_name` is returned to the caller.

### 4. Delete the Customer
Using either the `admin` or the `blue` scoped JWT token, you can delete the customer record. If using the `blue` scoped JWT token, *only* the customer object identified by the encoded `user_name` can be deleted. To run with the `blue` scoped JWT token to delete the user, run the command below:
Where:
* `${CUSTOMER_HOST}` is the hostname/ip address for the customer microservice.
* `${CUSTOMER_PORT}` is the port for the customer microservice.
* `${CUSTOMER_ID}` is the id of the customer user crated earlier, i.e. `41757d0170344f9ea47a2d9634bc9ba7`.
* `${jwt_blue}` is the JWT token created in the previous step.

```bash
curl -X DELETE -i "http://${CUSTOMER_HOST}:${CUSTOMER_PORT}/customer/delete/${CUSTOMER_ID}" -H "Content-type: application/json" -H "Authorization: Bearer ${jwt_blue}"

HTTP/1.1 200 OK
Date: Mon, 20 Aug 2018 22:20:00 GMT
X-Application-Context: customer-microservice:8082
Content-Length: 0
Server: Jetty(9.2.13.v20150730)
```

If successful, you should get a `200 OK` status code as shown in the command above.

### Deploy the CouchDB Docker Container
The easiest way to get CouchDB running is via a Docker container. To do so, run the following commands:
```bash
# Start a CouchDB Container with a database user, a password, and create a new database
docker run --name customercouchdb -p 5985:5984 -e COUCHDB_USER=admin -e COUCHDB_PASSWORD=passw0rd -d couchdb:2.1.2

Then visit http://127.0.0.1:5985/_utils/#login

# Get the CouchDB Container's IP Address
docker inspect customercouchdb | grep "IPAddress"
            "SecondaryIPAddresses": null,
            "IPAddress": "172.17.0.2",
                    "IPAddress": "172.17.0.2",
```
Make sure to select the IP Address in the `IPAddress` field. You will use this IP address when deploying the Customer container.

    
### Deploy the Customer backend to Openshift

    docker login --username $DOCKER_USERNAME --password $DOCKER_PASSWORD
    oc login --token=$YOUR_API_TOKEN --server=$CLUSTER_IP_ADDRESS
    oc new-project=customer-api
    appsody deploy -t $DOCKER_REGISTRY/$DOCKER_REPO:$VERSION --push --namespace customer-api 

Where `${COUCHDB_IP_ADDRESS}` is the IP address of the CouchDB container, which is only accessible from the Docker container network.

If everything works successfully, you should be able to get some data when you run the following command:
```bash
curl http://localhost:8080/customer
```

## Run Customer Service application on localhost
In this section you will run the Spring Boot application on your local workstation. Before we show you how to do so, you will need to deploy a CouchDB Docker container as shown in the [Deploy a CouchDB Docker Container](#deploy-a-couchdb-docker-container).

Once CouchDB is ready, we can run the Spring Boot Customer application locally as follows:


1. Build and run the application:
```bash
appsody run --docker-options "-e COUCHDB_PORT=5985 -e COUCHDB_HOST=host.docker.internal -e COUCHDB_PROTOCOL=http -e COUCHDB_USERNAME=admin -e COUCHDB_PASSWORD=passw0rd -e COUCHDB_DATABASE=customers -e HS256_KEY=E6526VJkKYhyTFRFMC0pTECpHcZ7TGcq8pKsVVgz9KtESVpheEO284qKzfzg8HpWNBPeHOxNGlyudUHi6i8tFQJXC8PiI48RUpMh23vPDLGD35pCM0417gf58z5xlmRNii56fwRCmIhhV7hDsm3KO2jRv4EBVz7HrYbzFeqI45CaStkMYNipzSm2duuer7zRdMjEKIdqsby0JfpQpykHmC5L6hxkX0BT7XWqztTr6xHCwqst26O0g8r7bXSYjp4a"
```

3. Validate. You should be able to do API calls via swagger by visiting
```bash
http://localhost:8080/swagger-ui.html#/
```

That's it, you have successfully deployed and tested the Customer microservice.

## Conclusion
You have successfully deployed and tested the Customer Microservice and a CouchDB database both on a Kubernetes Cluster and in local Docker Containers.

To see the Customer app working in a more complex microservices use case, checkout our Microservice Reference Architecture Application [here](https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/tree/spring).

## Contributing
If you would like to contribute to this repository, please fork it, submit a PR, and assign as reviewers any of the GitHub users listed here:
git clone https://github.com/ibm-garage-ref-storefront/customer-ms-spring

