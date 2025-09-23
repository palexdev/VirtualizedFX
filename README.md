<!--@formatter:off-->
[![HitCount](https://hits.dwyl.com/palexdev/VirtualizedFX.svg?style=flat-square)](http://hits.dwyl.com/palexdev/VirtualizedFX)
![GitHub Workflow Status](https://github.com/palexdev/virtualizedfx/actions/workflows/gradle.yml/badge.svg)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.palexdev/virtualizedfx?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)
[![javadoc](https://javadoc.io/badge2/io.github.palexdev/virtualizedfx/javadoc.svg?logo=java)](https://javadoc.io/doc/io.github.palexdev/virtualizedfx)
![GitHub issues](https://img.shields.io/github/issues-raw/palexdev/virtualizedfx?style=flat-square)
![GitHub pull requests](https://img.shields.io/github/issues-pr/palexdev/virtualizedfx?style=flat-square)
![GitHub](https://img.shields.io/github/license/palexdev/virtualizedfx?style=flat-square)
---

<!-- PROJECT LOGO -->
<br />
<p align="center">
  <a href="https://github.com/palexdev/VirtualizedFX">
    <img src=https://imgur.com/0Ae689U.png" alt="Logo">
  </a>
</p>


<h3 align="center">VirtualizedFX</h3>

<p align="center">
    VirtualizedFX is an open source Java library which provides virtualized components to display huge amounts of data
without hurting performance by rendering only a portion of it
    <br />
    <a href="https://github.com/palexdev/VirtualizedFX/tree/main/wiki"><strong>Explore the wiki »</strong></a>
    <br />
    <br />
    <a href="https://github.com/palexdev/VirtualizedFX/issues">Report Bug</a>
    ·
    <a href="https://github.com/palexdev/VirtualizedFX/issues">Request Feature</a>
</p>

<!-- TABLE OF CONTENTS -->

## Table of Contents

* [About the Project](#about-the-project)
* [Getting Started](#getting-started)
    * [Build](#build)
    * [Usage](#usage)
        * [Gradle](#gradle)
        * [Maven](#maven)
* [Documentation](#documentation)
* [Contributing](#contributing)
* [License](#license)
* [Contact](#contact)
* [Donation](#donation)
* [Supporters](#supporters)

<!-- ABOUT THE PROJECT -->

## About The Project

In UI there is a little-known concept called 'virtualization' which essentially is a technique to render only a portion
of elements generated from a larger number of data items. Typically used to display huge collections of items in UI
containers which can vary from lists, to grids, tables, trees, etc.  
Most of the time the algorithm revolves around the concept of 'fixed size' for all the elements, to predict exactly how
many items and UI nodes to create and show, how many pixels we need to display all the content and more. This property
not only makes things easier to compute but also a lot faster.
As of today, VirtualizedFX only offers solutions based on such concept, but I'd like to explore 'variable size' in the future.  
_Some articles on the matter:
[[1]](https://medium.com/ingeniouslysimple/building-a-virtualized-list-from-scratch-9225e8bec120),
[[2]](https://dev.to/murilovarela/make-your-virtualized-list-5cpm),
[[3]](https://dev.to/nishanbajracharya/what-i-learned-from-building-my-own-virtualized-list-library-for-react-45ik),
[[4]](https://betterprogramming.pub/virtualized-rendering-from-scratch-in-react-34c2ad482b16)._

VirtualizedFX is not the first nor the second product that offers UI virtualization.  
JavaFX has its own implementation, called VirtualFlow, which is an undocumented big blob class written following the
[Spaghetti Code](https://en.wikipedia.org/wiki/Spaghetti_code) anti-pattern.  
Other frameworks I did try had more or less the same issues, bad documentation, bad modularity, but also a high memory
footprint, old/unmaintained unnecessary dependencies.

Unlike others, VirtualizedFX is founded on a series of principles that make it more appealing and performant.
The code base is well documented, well organized and fairly simple, which makes it easier to maintain and understand.
That said, VirtualizedFX still brings a couple of dependencies with it to make the development easier:
[MFXCore](https://github.com/palexdev/MaterialFX/tree/rewrite/modules/core),
[MFXEffects](https://github.com/palexdev/MaterialFX/tree/rewrite/modules/effects),
[MFXResources](https://github.com/palexdev/MaterialFX/tree/rewrite/modules/resources).

<!-- GETTING STARTED -->

## Getting Started

In this section you can learn what do you need to use my library in your project.  
**Note:** the new version of VirtualizedFX, as the versioning 25.x.x may suggest, requires at least JDK 25.

### Build

To build VirtualizedFX, execute the following command:
```groovy
gradlew build
```

### Usage

###### Gradle

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.palexdev:virtualizedfx:25+'
}
```

###### Maven

```xml

<dependency>
    <groupId>io.github.palexdev</groupId>
    <artifactId>virtualizedfx</artifactId>
    <version>25.x.x</version>
</dependency>
```

<!-- DOCUMENTATION -->

## Documentation

You can read VirtualizedFX's documentation at [javadoc.io](https://javadoc.io/doc/io.github.palexdev/virtualizedfx)

<!-- CONTRIBUTING -->

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any
contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<!-- LICENSE -->

## License

Distributed under the GNU LGPLv3 License. See `LICENSE` for more information.

<!-- CONTACT -->

## Contact

Alex - alessandro.parisi406@gmail.com
<br></br>
[Discussions](https://github.com/palexdev/VirtualizedFX/discussions)
<br></br>
Project Link: [https://github.com/palexdev/VirtualizedFX](https://github.com/palexdev/VirtualizedFX)

#### Donation

VirtualizedFX is complex library, because it revolves around complex concept which is UI virtualization.
Still, by following strict rules, by documenting **everything** and testing it properly, I tried to make it as easy to
use and understand as possible. With the recent rewrite I can finally say I'm proud of my product, and I'm more than happy
to share it to you, make good use of it!  
The issue I have always had ever since I started writing this library, is the **insane** amount of time it takes to complete
even a single virtualized component. More than once I had to write almost all day long to get some results, and it was
really stressful.  
So, if you are using VirtualizedFX in your projects and feel like it, you can show your appreciation with a donation,
even a small one is **greatly appreciated**.
You can do it on [KoFi](https://ko-fi.com/palexdev) or with [PayPal](https://www.paypal.com/paypalme/alxpar404/2)

# Supporters:

(If you want your github page to be linked here and you didn't specify your username in the donation, feel free to
contact me by email and tell me. Also contact me if for some reason you don't want to be listed here)







