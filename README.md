[![Hits](https://hits.sh/github.com/palexdev/VirtualizedFX.svg)](https://hits.sh/github.com/palexdev/VirtualizedFX/)[![GitHub Workflow Status](https://github.com/palexdev/VirtualizedFX/actions/workflows/gradle.yml/badge.svg)](https://github.com/palexdev/VirtualizedFX/actions/workflows/gradle.yml)
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/io.github.palexdev/virtualizedfx?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)
![GitHub issues](https://img.shields.io/github/issues-raw/palexdev/virtualizedfx?style=flat-square)
![GitHub pull requests](https://img.shields.io/github/issues-pr/palexdev/virtualizedfx?style=flat-square)
![GitHub](https://img.shields.io/github/license/palexdev/virtualizedfx?style=flat-square)
---

<h3 align="center">VirtualizedFX</h3>
VirtualizedFX is yet another attempt at creating an efficient and flexible virtual flow for JavaFX. For those who don't
know what a VirtualFlow is I suggest you reading these articles which also helped me a lot in the realization of
VirtualizedFX:
<a href="https://medium.com/ingeniouslysimple/building-a-virtualized-list-from-scratch-9225e8bec120">[1]</a>,
<a href="https://dev.to/murilovarela/make-your-virtualized-list-5cpm">[2]</a>,
<a href="https://dev.to/nishanbajracharya/what-i-learned-from-building-my-own-virtualized-list-library-for-react-45ik">[3]</a>
,
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
  if not always you will end up using a JavaFX's Pane and implement the Cell interface.
- Cells are always reused, this makes the code easier, and the Virtual Flow more efficient as creating new Nodes is way
  more heavy on performance rather than updating them, consider that the user can scroll at very fast speeds, and
  creating new cells at those speeds is a no go for a component that has efficiency as a top priority.
  Storing them in memory is also a no go, you always have to consider edge cases, in which for example you could have
  thousands
  or millions of items, this will likely lead to an OutOfMemoryException
- There are no empty cells. The flow is programmed to always produce the amount of cells it needs no less no more.

<h3 align="center">VirtualGrid</h3>
VirtualGrid is the basic implementation of a virtualized Grid offered by VirtualizedFX. The peculiarity of this control
is for sure the backing data structure that keeps the items, the ObservableGrid offered by MFXCore.
This control is mostly useful when implementing galleries and similar, but perhaps it can also be used to represent
tabular data in some way.

- It uses the same cell system as VirtualFlow but the API is extended to allow cells to also know
  which are its coordinates in the viewport
- Just like VirtualFlow cells are reused for the maximum performance, and there are no empty cells

<h3 align="center">VirtualTable</h3>
VirtualTable is the basic implementation of a virtualized Table offered by VirtualizedFX. This is the
idea control to display tabular data. Even though the basics are the same as VirtualFlow, the table's
structure and mechanism is actually more complex.

- Just like for VirtualFlow, Cells are just dumb objects. The cell system is the same but the API
  has been extended to allow cells to get information such as the row that contains it and the column
  which created it. Also, VirtualTable makes it easier for the user to adapt to different data models.
  The extended API in combination with some provided default implementations allows the user to easily
  use the table with models that make use of JavaFX's properties, but also with those that do not
- Cells are not directly laid out in the viewport, instead they are organized in rows. TableRows are
  more than just "organizers", in fact they are JavaFX nodes, their purpose is to contain the cells
  produced by each column, as well as helping with scrolling and various types of updates
- Columns on the other hand can be considered more dummy compared to the rows. Their only function is
  to be headers for the cells. The provided default columns do not even provide APIs to sort or filter
  the table. However, implementing them is very easy, the API is very similar to Cells
- So, the table is organized mainly by rows. The thing I'm kinda proud of is that with this table
  it's also possible to highlight columns (at least with the default implementation), also I made
  some experiments and I found out that implementing the Drag-and-Drop feature should be very easy
- There's also another brand-new feature. This table implementation allows you to choose between two
  different modes on how to lay out the columns. The FIXED mode makes all the columns have the same width,
  making the table more efficient as some computations are simply faster; the VARIABLE mode allows columns
  to have different widths, it's suggested to not use this mode when the table has A LOT (it's a pretty high
  number anyway) of columns. This mode also enables features like auto-sizing and re-sizing at runtime with
  gestures

<h3 align="center">Paginated Variants</h3>
All the above virtualized controls also have a variant that allows to organize the items in pages. These are tipically
simple and naive implementations but they work pretty well. They usually have three new main properties:

- The current page property controls at which page the viewport is, so effectively it controls the position
- The max page reachable
- The number of rows to show per each page, this is also settable via CSS

Also, it's worth mentioning that with these variants it may happen to have more cells built than needed.
To be precise, this typically happens when for the last page there are not enough items to fill it, the cells in excess
are not deleted/disposed but kept in the viewport. This simplifies a lot the layout handling.

<h3 align="center">VirtualTree</h3>
I really want to implement this at some point, but the last few months passed developing VirtualizedFX have exhausted
me,
so I think for now I'll take a break from this. No worry, I'll keep maintaining the project of course, I'm just too
spent
to implement new features now.

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
  implementation 'io.github.palexdev:virtualizedfx:11.8.4'
}
```

###### Maven

```xml

<dependency>
    <groupId>io.github.palexdev</groupId>
  <artifactId>virtualizedfx</artifactId>
  <version>11.8.4</version>
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
contact me by email and tell me. Also contact me if for some reason you don't want to be listed here)

- *Your name can be here by supporting me at this link, [Support](https://bit.ly/31XB8zD)*
