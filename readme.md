# Final Projects for Networked Software for Distributed System Course at Politecnico di Milano - AY 2023-2024
Final projects for the course **"Networked Software for Distributed System"** at Politecnico di Milano (2023/2024).

## Author:
- Mormile Matteo ([@teomormi](https://github.com/teomormi))

## Project specification
The projects consist of the development of two distributed applications using Apache Kafka and Akka.
* ([Akka](https://akka.io/))
* ([Apache Kafka](https://kafka.apache.org/))

### A Simple Stream Processing System - Akka
The project is based on the construction of a simple stream processing system using actor model. 
The system is made up of a series of operators that use mathematical operators to gradually alter our data flow. Each operator in the pipeline is defined using a window (size, slide) and an aggregation function to process values in the current window, and each message passing through the operators contains a pair <key, value>. Upon completion of the window, the operator calculates the total value and transfers the outcome to the subsequent operator. The slide indicates how much data needs to be received before the operation is applied again. There are numerous independent streams in our system stream processing where the same kind of operators may appear repeatedly but in different instances.

For example, consider a pipeline to process sensor data, keys represent the type of data (temperature and humidity) and values are the actual sensor reading. Aggregation functions are operators like average, max and are applied successively to the data produced by the sensor. 

### Online services for continuous evaluation - Apache Kafka
The project is based on the implementation of an online application that support university courses adopting continuous evaluation.
The programme is made up of a frontend that receives user requests and a backend that handles them. Three categories of users interact with the service: professors, admin and students. 
The four services that compose the backend communicate with each other via Kafka and are:
- the user service which is responsible for managing registered users' personal information;
- the courses service manages courses, which are made up of multiple projects;
- the projects service handles the submission and grading of individual projects;
- the registration service registers final grades for courses that have been completed (a course is considered completed for a student if they have completed all of the projects).
Each user, depending on their role, will access a different dashboard that will allow them to perform different functions such as register for courses, submit solutions for projects, create or remove courses and others.

## Copyright and license
Licensed under the **[MIT License](https://github.com/teomormi/networked-software-distributed-systems-projects/blob/main/LICENSE)**;
you may not use this software except in compliance with the License.

[license]: https://github.com/teomormi/networked-software-distributed-systems-projects/blob/main/LICENSE
[license-image]: https://img.shields.io/badge/License-MIT-blue.svg
