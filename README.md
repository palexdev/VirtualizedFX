[![HitCount](http://hits.dwyl.com/PAlex404/VirtualizedFX.svg)](http://hits.dwyl.com/PAlex404/VirtualizedFX)
[![GitHub Workflow Status](https://github.com/palexdev/VirtualizedFX/actions/workflows/gradle.yml/badge.svg)](https://github.com/palexdev/VirtualizedFX/actions/workflows/gradle.yml)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.palexdev/virtualizedfx?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)
![GitHub issues](https://img.shields.io/github/issues-raw/palexdev/virtualizedfx?style=flat-square)
![GitHub pull requests](https://img.shields.io/github/issues-pr/palexdev/virtualizedfx?style=flat-square)
![GitHub](https://img.shields.io/github/license/palexdev/virtualizedfx?style=flat-square)
---

<h3 align="center">VirtualizedFX</h3>
VirtualizedFX is yet another attempt at creating an efficient and flexible virtual flow for JavaFX. For those who don't
know what a VirtualFlow is I suggest you reading these articles which also helped me a lot in the realuzation of
VirtualizedFX:
<a href="https://medium.com/ingeniouslysimple/building-a-virtualized-list-from-scratch-9225e8bec120">[1]</a>,
<a href="https://dev.to/murilovarela/make-your-virtualized-list-5cpm">[2]</a>,
<a href="https://dev.to/nishanbajracharya/what-i-learned-from-building-my-own-virtualized-list-library-for-react-45ik">[3]</a>,
<a href="https://betterprogramming.pub/virtualized-rendering-from-scratch-in-react-34c2ad482b16">[4]</a>.
<p></p>

Unlike other VirtualFlows, VirtualizedFX's code is well documented, well organized and fairly simple which not only
makes the project easier to maintain but also easier to understand. As of version 11.5.0, it has two dependencies which
are
[MFXCore](https://github.com/palexdev/MFXCore) and [MFXResources](https://github.com/palexdev/MFXResources). They bring
a bunch of utilities, beans and icons for the needed controls and to make development easier.

As of now VirtualizedFX offers only a basic Virtual Flow to implement ListViews, TableViews and maybe also TreeViews.
The cells must have fixed sizes depending on the orientation (fixed width for horizontal flows, and fixed height for
vertical flows) and this is the most efficient form of Virtual Flow. In the future I would also like to implement more
complex Virtual Flows, one that doesn't care about cells sizes, one specifically for TreeViews and who knows what else.
<p></p>
As of version 11.5.0 the fixed cell size is managed directly by the virtual flow making it possible to change the cells'
size at anytime, even at runtime.
<p></p>

<h3 align="center">VirtualFlow</h3>
VirtualFlow is the basic implementation of a Virtual Flow offered by VirtualizedFX. Without talking about that
blob/spaghetti coding that is the JavaFX VirtualFlow, let's compare it to Flowless.

- The base idea of cells is the same. For VirtualizedFX Cells are just dumb controls they do not have any logic and they
  aren't even nodes. This makes the Virtual Flow extremely flexible and efficient. However, note that, in most occasions
  if
  not always you will end up using a JavaFX's Pane and implement the Cell interface.
- Cells are always reused, this makes the code easier, and the Virtual Flow more efficient as creating new Nodes is way
  more heavy on performance rather than updating them, consider that the user can scroll at very fast speeds, and
  creating new cells at those speeds is a no go for a component that has efficiency as a top priority.
  Storing them in memory is also a no go, you always have to consider edge cases, in which for example you could have
  thousands
  or millions of items, this will likely lead to an OutOfMemoryException

  // TODO here
- There are empty cells, or to be more precise, they are hidden if they are not needed. This allows to do some cool
  tricks with the Virtual Flow, for example you could create paginated lists or tables. In such cases for example the
  last page may not have enough items to fill the viewport so the unneeded cells are hidden.

<p></p>

<!-- GETTING STARTED -->

## Getting Started

In this section you can learn what do you need to use my library in your projects.

### Build

To build VirtualizeFX, execute the following command:

    gradlew build

**NOTE** : VirtualizedFX requires **Java 11** and above.

### Usage

###### Gradle

```groovy
repositories {
    mavenCentral()
}

dependencies {
  implementation 'io.github.palexdev:virtualizedfx:11.2.6'
}
```

###### Maven

```xml

<dependency>
    <groupId>io.github.palexdev</groupId>
  <artifactId>virtualizedfx</artifactId>
  <version>11.2.6</version>
</dependency>
```

<p></p>

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
[![Discord](https://img.shields.io/discord/771702793378988054?label=Discord&style=flat-square)](https://discord.com/invite/zFa93NE)
<br /><br />
Project Link: [https://github.com/palexdev/VirtualizedFX](https://github.com/palexdev/VirtualizedFX)

<!-- DONATION -->

#### Donation

Implementing a new Virtual Flow from scratch has been a really hard task, such a low level component. But the
satisfaction in having today something so complex that works so good is immense. If you are using VirtualizedFX in your
projects and feel like it, you can make a small donation here:
[![Donate](https://img.shields.io/badge/$-support-green.svg?style=flat-square)](https://bit.ly/31XB8zD), it would really
make my day.

<!-- SUPPORTERS -->

# Supporters:

(If you want your github page to be linked here and you didn't specify your username in the donation, feel free to
contact me by email and tell me. Also contact me if for some some reason you don't want to be listed here)

- *Your name can be here by supporting me at this link, [Support](https://bit.ly/31XB8zD)*
