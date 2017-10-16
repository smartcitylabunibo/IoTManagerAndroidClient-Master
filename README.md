## IoTManagerAndroidClient-Master ##
An Android Client for the IoT Manager framework (master repository)

## Synopsis ##
IoT Manager is a general framework which allows users to deal with heterogeneous sensor networks within the smart city context.
It is composed by a client component (front-end) which delivers useful information gathered by sensors (depending on the user's position and settings) and by a server component (back-end) which collects data from sensor networks and exposes them to the front-end.
The framework natively manages georeferencing from both the user's and the sensors' perspective.

## Usage ##
This repository contains an Android project implementing a client application for the IoT Manager framework.
Once installed, the client app can communicate with the framework's back-end to retrieve data collected by different sensors.

In order to address requests to the back-end correctly, the user needs to be provided with a valid username and the corresponding password.
These credentials are mainteined and granted by the [Smart City Lab](http://smartcity.csr.unibo.it) - Università di Bologna, Campus di Cesena, Italy.

This project has a strong teaching aim. Specifically, it is used as a training platform within the ["Smart City e Tecnologie Mobili"](http://smartcity.csr.unibo.it/smart-city-e-tecnologie-mobili/) master's degree course, held at the Università di Bologna, Campus di Cesena, [Corso di Laurea in Scienze e Tecnologie Informatiche](http://corsi.unibo.it/scienzetecnologieinformatiche/Pagine/default.aspx).

The IoTManagerAndroidClient-Student repository contains a limited version of the client which needs to be expanded and completed by students during the course classes.

## Bibliography ##
IoT Manager was designed and proposed throughout several original research papers:
- Luca Calderoni, Dario Maio, Stefano Rovis: *Deploying a network of smart cameras for traffic monitoring on a “city kernel”.* Expert Systems with Applications, vol. 41, pp. 502-507, February 2014. ISSN 0957-4174
- Luca Calderoni, Antonio Magnani, Dario Maio: *Coupling a UDOO-based weather station with IoT Manager*. I-Cities 2017. Proceedings of the 3rd Italian Conference on ICT for Smart Cities & Communities, September 2017.

## Authors ##
Luca Calderoni, Antonio Magnani - Università di Bologna (Italy)

## Scientific supervisor ##
Dario Maio - Università di Bologna (Italy)

## License ##
The source code is released under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Please refer to the included files [COPYING](COPYING) and [COPYING.LESSER](COPYING.LESSER).

## Acknowledgements ##
This project relies on some existing libraries. Specifically:
- The graphical user interface relies on the MPAndroidChart project for what concerning charts. Please refer to [the project home page](https://github.com/PhilJay/MPAndroidChart) for MPAndroidChart licence details.
- Maps features rely on [Google Maps API v2](https://developers.google.com/maps/documentation/android-api/). In order to turn on these functionalities it is mandatory to fill the [strings.xml](/IoTManagerClientMain/src/main/res/values/strings.xml) file with a valid API Key.
